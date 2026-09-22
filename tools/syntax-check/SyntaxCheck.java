import com.sun.source.util.JavacTask;
import java.nio.file.*;
import java.util.*;
import javax.tools.*;
/** Parse only; deliberately does not pretend to type-check missing Minecraft dependencies. */
public final class SyntaxCheck {
    public static void main(String[] args) throws Exception {
        Path root=Path.of(args.length==0?".":args[0]);
        var compiler=ToolProvider.getSystemJavaCompiler();
        if(compiler==null)throw new IllegalStateException("A JDK is required");
        var diagnostics=new DiagnosticCollector<JavaFileObject>();
        try(var manager=compiler.getStandardFileManager(diagnostics,Locale.ROOT,java.nio.charset.StandardCharsets.UTF_8);
            var files=Files.walk(root)) {
            var paths=files.filter(p->p.toString().endsWith(".java")&&!p.toString().contains("/build/")).toList();
            var units=manager.getJavaFileObjectsFromPaths(paths);
            var task=(JavacTask)compiler.getTask(null,manager,diagnostics,List.of("-proc:none"),null,units);
            task.parse();
            var errors=diagnostics.getDiagnostics().stream().filter(d->d.getKind()==Diagnostic.Kind.ERROR).toList();
            errors.forEach(System.err::println);
            if(!errors.isEmpty())throw new IllegalStateException("Java parser errors: "+errors.size());
            System.out.println("JAVA SYNTAX PASS: "+paths.size()+" files parsed. No Minecraft type check or game launch.");
        }
    }
}
