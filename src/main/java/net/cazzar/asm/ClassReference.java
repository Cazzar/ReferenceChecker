package net.cazzar.asm;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.ListIterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ClassReference {
    public static void main(String[] args) throws IOException {
        OptionParser parser = new OptionParser();
        parser.accepts("in", "the jar to use").withRequiredArg().ofType(File.class).required();
        parser.accepts("class", "the class to check for").withRequiredArg().ofType(String.class);
        parser.accepts("field", "the field to check for owner.name").withRequiredArg().ofType(String.class);
        parser.accepts("method", "the java annotated method to check for owner.name(Ljava/lang/object;)Z").withRequiredArg().ofType(String.class);
        parser.accepts("return", "use only return values");

        OptionSet optionSet = null;
        try {
            optionSet = parser.parse(args);
        } catch (OptionException e) {
            parser.printHelpOn(System.out);
            System.exit(1);
        }


        File jar = (File) optionSet.valueOf("in");
        ZipFile zip = new ZipFile(jar);
        Enumeration<? extends ZipEntry> entries = zip.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;


            ClassNode node = new ClassNode();
            ClassReader reader = new ClassReader(zip.getInputStream(entry));
            reader.accept(node, 0);

            if (optionSet.has("class")) {
                String ref = ((String) optionSet.valueOf("class")).replaceAll("\\.", "/");
                checkForClassRef(ref, node, entry.getName());
            }
            else if (optionSet.has("field")) {
                String[] ref = ((String) optionSet.valueOf("field")).split("\\.");
                checkForFieldRef(ref[0], ref[1], node, entry.getName());
            }
            else if (optionSet.has("method")) {
                String ref = (String) optionSet.valueOf("method");
                String owner = ref.split("\\.")[0];
                String name = ref.split("\\.")[1];
                final int beginIndex = name.indexOf('(');
                String desc = name.substring(beginIndex);
                name = name.substring(0, beginIndex);

                checkForMethodRef(owner, name, desc, node, entry.getName());
            }
        }
    }

    private static void checkForClassRef(String ref, ClassNode node, String name) {
        if (node.superName.equals(ref))
            System.out.println("Class super: " + name);

        if (node.interfaces.contains(ref))
            System.out.println("Class Interface: " + name);

        for (MethodNode method : node.methods) {
            if (method.desc.endsWith("L" + ref + ";"))
                System.out.println("Return: " + name + " " + method.name + method.desc);

            final ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();
            while (iterator.hasNext()) {
                AbstractInsnNode insn = iterator.next();

                if (insn instanceof MethodInsnNode && ((MethodInsnNode) insn).desc.endsWith("L" + ref + ";"))
                    System.out.println("Reference in: " + name + " " + method.name + method.desc);

                else if (insn instanceof FieldInsnNode && ((FieldInsnNode) insn).desc.equals("L" + ref + ";"))
                    System.out.println("Reference in: " + name + " " + method.name + method.desc);

                else if (insn instanceof MultiANewArrayInsnNode && ((MultiANewArrayInsnNode) insn).desc.equals(ref))
                    System.out.println("Reference in: " + name + " " + method.name + method.desc);

                else if (insn instanceof TypeInsnNode && insn.getOpcode() == Opcodes.NEWARRAY && ((TypeInsnNode) insn).desc.equals(ref))
                    System.out.println("Reference in: " + name + " " + method.name + method.desc);
            }
        }
    }

    private static void checkForFieldRef(String owner, String name, ClassNode node, String ent) {
        for (MethodNode method : node.methods) {
            final ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();
            while (iterator.hasNext()) {
                AbstractInsnNode insn = iterator.next();

                final FieldInsnNode insnNode = insn instanceof FieldInsnNode ? ((FieldInsnNode) insn) : null;
                if (insnNode == null) continue;

                if (insnNode.owner.equals(owner) && insnNode.name.equals(name))
                    System.out.println("Reference in: " + ent + " " + method.name + method.desc);
            }
        }
    }

    private static void checkForMethodRef(String owner, String name, String desc, ClassNode node, String ent) {
        for (MethodNode method : node.methods) {
            final ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();
            while (iterator.hasNext()) {
                AbstractInsnNode insn = iterator.next();

                final MethodInsnNode insnNode = insn instanceof MethodInsnNode ? ((MethodInsnNode) insn) : null;
                if (insnNode == null) continue;

                if (insnNode.owner.equals(owner) && insnNode.name.equals(name) && insnNode.desc.equals(desc))
                    System.out.println("Reference in: " + ent + " " + method.name + method.desc);
            }
        }
    }
}
