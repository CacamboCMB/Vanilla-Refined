#!/usr/bin/env python3
from pathlib import Path
import subprocess, tempfile, shutil, sys

ROOT = Path(__file__).resolve().parents[2]
HELPERS = ROOT / 'settings/src/client/java/de/ownmods/settings/client/compat'

def run(args):
    r = subprocess.run(args, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=40)
    if r.returncode:
        raise RuntimeError(f"Failed ({r.returncode}): {' '.join(args)}\n{r.stdout}")
    return r.stdout

def write(root, relative, source):
    p = root / relative
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(source, encoding='utf-8')

def base_itemstack(root):
    write(root, 'net/minecraft/world/item/ItemStack.java', '''
package net.minecraft.world.item;
public class ItemStack {
    public static final ItemStack EMPTY = new ItemStack(-1);
    public final int id;
    public ItemStack(int id) { this.id = id; }
    public ItemStack copy() { return new ItemStack(id); }
}
''')

def fixture(root, modern):
    package = 'com.mojang.renderpearl.api.pipeline' if modern else 'com.mojang.blaze3d.pipeline'
    write(root, package.replace('.', '/') + '/RenderPipeline.java',
          f'package {package}; public final class RenderPipeline {{}}')
    write(root, 'net/minecraft/resources/Identifier.java',
          'package net.minecraft.resources; public final class Identifier {}')
    write(root, 'com/mojang/blaze3d/platform/Window.java',
          'package com.mojang.blaze3d.platform; public final class Window {}')
    enum_value = 'KEYBOARD' if modern else 'KEYSYM'
    signature = 'int key' if modern else 'Window window, int key'
    guard = '' if modern else 'if(window == null) throw new IllegalArgumentException("window missing");'
    write(root, 'com/mojang/blaze3d/platform/InputConstants.java', f'''
package com.mojang.blaze3d.platform;
public final class InputConstants {{
    public enum Type {{ {enum_value}, MOUSE }}
    public static final int KEY_M = {16 if modern else 77};
    public static final int KEY_LSHIFT = {225 if modern else 340};
    public static final int KEY_RSHIFT = {229 if modern else 344};
    public static final int MOUSE_BUTTON_LEFT = {1 if modern else 0};
    public static final int MOUSE_BUTTON_RIGHT = {3 if modern else 1};
    public static final String KEY_WRONG_TYPE = "not int";
    public static boolean isKeyDown({signature}) {{ {guard} return key == KEY_LSHIFT; }}
}}
''')
    write(root, 'net/minecraft/client/renderer/RenderPipelines.java', f'''
package net.minecraft.client.renderer;
public final class RenderPipelines {{
    public static final {package}.RenderPipeline GUI_TEXTURED = new {package}.RenderPipeline();
}}
''')
    write(root, 'fixture/Gfx.java', f'''
package fixture;
import {package}.RenderPipeline;
import net.minecraft.resources.Identifier;
public final class Gfx {{
    public int calls; public Object pipeline; public Object id; public float[] args;
    public boolean fail;
    private void record(RenderPipeline p, Identifier i, float... a) {{
        if (fail) throw new IllegalArgumentException("fixture failure");
        calls++; pipeline = p; id = i; args = a;
    }}
    public void blit(RenderPipeline p, Identifier i, int x, int y, float u, float v,
                     int w, int h, int tw, int th) {{ record(p, i, x, y, u, v, w, h, tw, th); }}
    public void blit(RenderPipeline p, Identifier i, int x, int y, float u, float v,
                     int w, int h, int rw, int rh, int tw, int th) {{ record(p, i, x, y, u, v, w, h, rw, rh, tw, th); }}
}}
''')
    write(root, 'fixture/OldContents.java', '''
package fixture;
import java.util.*; import java.util.stream.*; import net.minecraft.world.item.ItemStack;
public final class OldContents {
    private final List<ItemStack> items = List.of(new ItemStack(10), ItemStack.EMPTY, new ItemStack(12));
    public Stream<ItemStack> allItemsCopyStream() { return items.stream().map(ItemStack::copy); }
    public ItemStack original() { return items.get(0); }
}
''')
    write(root, 'fixture/IterableContents.java', '''
package fixture;
import java.util.*; import net.minecraft.world.item.ItemStack;
public final class IterableContents implements Iterable<ItemStack> {
    private final List<ItemStack> values = List.of(new ItemStack(20), ItemStack.EMPTY, new ItemStack(22));
    public Iterator<ItemStack> iterator() { return values.iterator(); }
    public ItemStack original() { return values.get(0); }
}
''')
    write(root, 'fixture/IndexedContents.java', '''
package fixture;
import net.minecraft.world.item.ItemStack;
public final class IndexedContents {
    private final ItemStack[] values = { new ItemStack(30), ItemStack.EMPTY, new ItemStack(32) };
    public ItemStack getItem(int slot) { return values[slot]; }
    public ItemStack original() { return values[0]; }
}
''')
    write(root, 'fixture/BackingContents.java', '''
package fixture;
import java.util.*; import net.minecraft.world.item.ItemStack;
public final class BackingContents {
    private final List<ItemStack> values = List.of(new ItemStack(40), ItemStack.EMPTY, new ItemStack(42));
    public ItemStack original() { return values.get(0); }
}
''')

HARNESS = r'''
import de.ownmods.settings.client.compat.InputCompat;
import de.ownmods.settings.client.compat.GuiTextureCompat;
import de.ownmods.settings.client.compat.ContainerContentsCompat;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import fixture.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class Harness {
    private static int checks;
    private enum UnknownType { OTHER }
    private static void check(boolean result, String name) {
        if (!result) throw new AssertionError(name); checks++;
    }
    private static void rejects(Runnable action, Class<? extends Throwable> expected, String label) {
        try { action.run(); } catch (Throwable e) {
            check(expected.isInstance(e), label + ": wrong error " + e); return;
        }
        throw new AssertionError(label + ": did not fail");
    }
    private static void checkContents(Object contents, ItemStack original, String prefix, int a, int c) {
        String strategy = ContainerContentsCompat.describeStrategy(contents.getClass());
        check(strategy.startsWith(prefix), "container strategy " + prefix + " got " + strategy);
        List<ItemStack> copy = ContainerContentsCompat.copyFirst(contents, 27);
        check(copy.size() == 3, "container slot count " + strategy);
        check(copy.get(0).id == a && copy.get(1).id == -1 && copy.get(2).id == c, "container slot order/empties " + strategy);
        check(copy.get(0) != original, "defensive ItemStack copy " + strategy);
    }
    public static void main(String[] args) {
        boolean modern = Boolean.parseBoolean(args[0]);
        check(InputCompat.code("KEY_M") == (modern ? 16 : 77), "runtime keyboard code");
        check(InputCompat.code("KEY_M") == (modern ? 16 : 77), "cached keyboard code");
        check(InputCompat.code("MOUSE_BUTTON_LEFT") == (modern ? 1 : 0), "left button");
        check(InputCompat.code("MOUSE_BUTTON_RIGHT") == (modern ? 3 : 1), "right button");
        check(InputCompat.keyboardType(InputConstants.Type.class).name().equals(modern ? "KEYBOARD" : "KEYSYM"), "enum rename");
        check(InputCompat.isKeyDown(new Window(), "KEY_LSHIFT"), "key state true");
        check(!InputCompat.isKeyDown(new Window(), "KEY_RSHIFT"), "key state false");
        rejects(() -> InputCompat.code(null), IllegalArgumentException.class, "null key");
        rejects(() -> InputCompat.code("INVALID"), IllegalArgumentException.class, "invalid key");
        rejects(() -> InputCompat.code("KEY_NOT_PRESENT"), IllegalStateException.class, "missing key");
        rejects(() -> InputCompat.code("KEY_WRONG_TYPE"), IllegalStateException.class, "wrong key field type");
        rejects(() -> InputCompat.keyboardType(UnknownType.class), IllegalStateException.class, "unknown keyboard type");

        Gfx g = new Gfx(); Identifier id = new Identifier();
        GuiTextureCompat.blit(g, id, 1, 2, 3f, 4f, 5, 6, 7, 8);
        check(g.calls == 1 && g.id == id && g.pipeline != null, "plain binding");
        check(Arrays.equals(g.args, new float[]{1,2,3,4,5,6,7,8}), "plain geometry");
        GuiTextureCompat.blit(g, id, 2, 4, 6f, 8f, 10, 12, 14, 16, 18, 20);
        check(g.calls == 2, "region binding");
        check(Arrays.equals(g.args, new float[]{2,4,6,8,10,12,14,16,18,20}), "region geometry");
        g.fail = true;
        rejects(() -> GuiTextureCompat.blit(g, id, 1, 2, 3f, 4f, 5, 6, 7, 8), IllegalArgumentException.class, "renderer error");
        rejects(() -> GuiTextureCompat.blit(new Object(), id, 1, 2, 3f, 4f, 5, 6, 7, 8), IllegalStateException.class, "unsupported gfx");

        OldContents oldC = new OldContents();
        IterableContents iterableC = new IterableContents();
        IndexedContents indexedC = new IndexedContents();
        BackingContents backingC = new BackingContents();
        checkContents(oldC, oldC.original(), "named-stream:", 10, 12);
        checkContents(iterableC, iterableC.original(), "java.lang.Iterable", 20, 22);
        checkContents(indexedC, indexedC.original(), "indexed-item:", 30, 32);
        checkContents(backingC, backingC.original(), "backing-field:", 40, 42);
        check(ContainerContentsCompat.copyFirst(null, 27).isEmpty(), "null contents");
        System.out.println("SYNTHETIC " + (modern ? "modern" : "legacy") + ": " + checks + " passed");
    }
}
'''

def main():
    if not shutil.which('java') or not shutil.which('javac'):
        raise SystemExit('A JDK with java and javac is required')
    with tempfile.TemporaryDirectory(prefix='vanilla-refined-compat-') as temp:
        work=Path(temp)
        common_src=work/'common-src'; common_src.mkdir()
        common=work/'common'; common.mkdir()
        base_itemstack(common_src)
        run(['javac','-encoding','UTF-8','-d',str(common),str(common_src/'net/minecraft/world/item/ItemStack.java')])
        helper_files=[HELPERS/'InputCompat.java', HELPERS/'GuiTextureCompat.java', HELPERS/'ContainerContentsCompat.java']
        run(['javac','-encoding','UTF-8','-cp',str(common),'-d',str(common),*map(str,helper_files)])
        for modern in (False,True):
            s=work/('modern-src' if modern else 'old-src'); c=work/('modern' if modern else 'old'); c.mkdir()
            fixture(s,modern)
            run(['javac','-encoding','UTF-8','-cp',str(common),'-d',str(c),*map(str,s.rglob('*.java'))])
        write(work,'Harness.java',HARNESS)
        sep=';' if sys.platform=='win32' else ':'
        run(['javac','-encoding','UTF-8','-cp',str(common)+sep+str(work/'old'),'-d',str(common),str(work/'Harness.java')])
        total=0
        for modern in (False,True):
            out=run(['java','-cp',str(common)+sep+str(work/('modern' if modern else 'old')),'Harness',str(modern).lower()]).strip()
            print(out)
    print('Synthetic input/render/container adapters passed on both fixture families. No real Minecraft/Fabric or in-game test was performed.')

if __name__ == '__main__':
    main()
