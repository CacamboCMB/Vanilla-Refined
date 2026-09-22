package de.ownmods.notes.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.function.Consumer;

/** Versioned local store; no Minecraft/server/world mutation. Failed loads are never overwritten. */
public final class NotesStore {
    public static final int MAX_NOTES = 1000;
    private static final long MAX_BYTES = 16*1024*1024;
    private final Path file;
    private final String scope;
    private LinkedHashMap<String, Note> notes = new LinkedHashMap<>();
    public NotesStore(Path file, String scope) throws IOException {
        this.file=Objects.requireNonNull(file); this.scope=Objects.requireNonNull(scope);
        if(Files.exists(file)) load();
    }
    public Path path() { return file; }
    public List<Note> all() { return notes.values().stream().map(Note::copy).toList(); }
    public Optional<Note> find(String id) { return Optional.ofNullable(notes.get(id)).map(Note::copy); }
    public static String scopeKey(String identity) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public void save(Note draft) throws IOException {
        Note n=draft.copy(); n.validate(); n.updated=System.currentTimeMillis();
        LinkedHashMap<String, Note> next=new LinkedHashMap<>(notes);
        next.put(n.id,n); if(next.size()>MAX_NOTES) throw new IOException("Maximal "+MAX_NOTES+" Notizen pro Welt");
        for(String link:n.links) if(!next.containsKey(link)) throw new IOException("Verknuepfte Notiz fehlt: "+link);
        commit(next);
    }
    public void delete(String id) throws IOException {
        LinkedHashMap<String, Note> next=new LinkedHashMap<>();
        for(Note n:notes.values()) if(!n.id.equals(id)) { Note c=n.copy(); c.links.remove(id); next.put(c.id,c); }
        commit(next);
    }
    private void commit(LinkedHashMap<String, Note> next) throws IOException {
        Properties p=new Properties(); p.setProperty("format","1"); p.setProperty("scope",scope); p.setProperty("count",""+next.size());
        int i=0; for(Note n:next.values()) write(p,"note."+(i++)+".",n);
        StringWriter writer=new StringWriter(); p.store(writer,"OwnMods Notizen v1 - lokale Spielnotizen");
        byte[] bytes=writer.toString().getBytes(StandardCharsets.UTF_8);
        if(bytes.length>MAX_BYTES) throw new IOException("Notizbuch zu gross (16 MiB)");
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path temp=Files.createTempFile(file.toAbsolutePath().getParent(),"notes-",".tmp");
        try {
            Files.write(temp,bytes,StandardOpenOption.TRUNCATE_EXISTING);
            try(var channel=java.nio.channels.FileChannel.open(temp,StandardOpenOption.WRITE)) {channel.force(true);}
            if(Files.exists(file)) Files.copy(file,file.resolveSibling(file.getFileName()+".bak"),StandardCopyOption.REPLACE_EXISTING);
            try { Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException e) { Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING); }
            notes=next;
        } finally {Files.deleteIfExists(temp);}
    }
    private void load() throws IOException {
        if(Files.size(file)>MAX_BYTES) throw new IOException("Notizdatei zu gross");
        Properties p=new Properties();
        try(Reader r=Files.newBufferedReader(file,StandardCharsets.UTF_8)) {p.load(r);}
        try {
            if(!"1".equals(p.getProperty("format"))) throw new IllegalArgumentException("Unbekannte Dateiversion");
            if(!scope.equals(p.getProperty("scope"))) throw new IllegalArgumentException("Notizdatei gehoert zu anderer Welt");
            int count=number(p,"count",0,MAX_NOTES);
            var loaded=new LinkedHashMap<String,Note>();
            for(int i=0;i<count;i++) {Note n=read(p,"note."+i+"."); if(loaded.put(n.id,n)!=null) throw new IllegalArgumentException("Doppelte Notiz-ID");}
            for(Note n:loaded.values()) for(String id:n.links) if(!loaded.containsKey(id)) throw new IllegalArgumentException("Verwaister Verweis");
            notes=loaded;
        } catch(RuntimeException e) {throw new IOException("Notizdatei wird zum Schutz nicht ueberschrieben: "+e.getMessage(),e);}
    }
    private static String text(Properties p,String k) { String v=p.getProperty(k); if(v==null)throw new IllegalArgumentException("Fehlendes Feld "+k);return v; }
    private static boolean bool(Properties p,String k) {String v=text(p,k); if(!v.equals("true")&&!v.equals("false"))throw new IllegalArgumentException("Ungueltiger Schalter");return Boolean.parseBoolean(v);}
    private static boolean bool(Properties p,String k,boolean fallback) {String v=p.getProperty(k);if(v==null)return fallback;if(!v.equals("true")&&!v.equals("false"))throw new IllegalArgumentException("Ungueltiger Schalter");return Boolean.parseBoolean(v);}
    private static int number(Properties p,String k,int min,int max) {int n=Integer.parseInt(text(p,k));if(n<min||n>max)throw new IllegalArgumentException("Ungueltige Anzahl "+k);return n;}
    private static void put(Properties p,String k,Object v) {p.setProperty(k,String.valueOf(v));}
    private static void position(Properties p,String k,Note.Position pos) {
        put(p,k+"present",pos!=null); if(pos!=null) {put(p,k+"dimension",pos.dimension());put(p,k+"x",pos.x());put(p,k+"y",pos.y());put(p,k+"z",pos.z());}
    }
    private static Note.Position position(Properties p,String k) {
        if(!bool(p,k+"present"))return null;
        return new Note.Position(text(p,k+"dimension"),number(p,k+"x",-30_000_000,30_000_000),number(p,k+"y",-2048,2048),number(p,k+"z",-30_000_000,30_000_000));
    }
    private static void write(Properties p,String k,Note n) {
        put(p,k+"id",n.id);put(p,k+"kind",n.kind);put(p,k+"title",n.title);put(p,k+"object",n.object);put(p,k+"biome",n.biome);
        put(p,k+"mob",n.mob);put(p,k+"goal",n.goal);put(p,k+"status",n.status);put(p,k+"favorite",n.favorite);put(p,k+"color",n.color);
        put(p,k+"farmBuilt",n.farmBuilt);put(p,k+"autoInventory",n.autoInventory);put(p,k+"trashed",n.trashed);put(p,k+"created",n.created);put(p,k+"updated",n.updated);
        position(p,k+"position.",n.position);position(p,k+"otherPortal.",n.otherPortal);
        put(p,k+"targets",n.targets.size()); for(int i=0;i<n.targets.size();i++){var t=n.targets.get(i);put(p,k+"t."+i+".item",t.item());put(p,k+"t."+i+".wanted",t.wanted());put(p,k+"t."+i+".count",t.manualCount());}
        put(p,k+"pages",n.pages.size());for(int i=0;i<n.pages.size();i++)put(p,k+"p."+i,n.pages.get(i));
        put(p,k+"checks",n.checks.size());for(int i=0;i<n.checks.size();i++){put(p,k+"c."+i+".text",n.checks.get(i).text());put(p,k+"c."+i+".done",n.checks.get(i).done());}
        put(p,k+"links",n.links.size());for(int i=0;i<n.links.size();i++)put(p,k+"l."+i,n.links.get(i));
    }
    private static Note read(Properties p,String k) {
        Note n=new Note();n.id=text(p,k+"id");n.kind=Note.Kind.valueOf(text(p,k+"kind"));n.title=text(p,k+"title");n.object=text(p,k+"object");
        n.biome=text(p,k+"biome");n.mob=text(p,k+"mob");n.goal=text(p,k+"goal");n.status=Note.Status.valueOf(text(p,k+"status"));
        n.favorite=bool(p,k+"favorite");n.color=text(p,k+"color");n.farmBuilt=bool(p,k+"farmBuilt");n.autoInventory=bool(p,k+"autoInventory");n.trashed=bool(p,k+"trashed",false);
        n.created=Long.parseLong(text(p,k+"created"));n.updated=Long.parseLong(text(p,k+"updated"));
        n.position=position(p,k+"position.");n.otherPortal=position(p,k+"otherPortal.");
        int ts=number(p,k+"targets",0,256);for(int i=0;i<ts;i++)n.targets.add(new Note.Target(text(p,k+"t."+i+".item"),number(p,k+"t."+i+".wanted",1,9999),number(p,k+"t."+i+".count",0,9999)));
        int ps=number(p,k+"pages",1,24);n.pages.clear();for(int i=0;i<ps;i++)n.pages.add(text(p,k+"p."+i));
        int cs=number(p,k+"checks",0,200);for(int i=0;i<cs;i++)n.checks.add(new Note.Check(text(p,k+"c."+i+".text"),bool(p,k+"c."+i+".done")));
        int ls=number(p,k+"links",0,100);for(int i=0;i<ls;i++)n.links.add(text(p,k+"l."+i));n.validate();return n;
    }
}
