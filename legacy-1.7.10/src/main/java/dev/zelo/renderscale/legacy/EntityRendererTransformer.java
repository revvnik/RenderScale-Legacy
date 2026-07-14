package dev.zelo.renderscale.legacy;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Brackets EntityRenderer.renderWorld with RenderScale's target swap.
 *
 * The target is selected through transformedName, while both production
 * (obfuscated) and development/SRG method names are accepted explicitly.
 */
public final class EntityRendererTransformer implements IClassTransformer {
    private static final String TARGET = "net.minecraft.client.renderer.EntityRenderer";
    private static final String RUNTIME =
            "dev/zelo/renderscale/legacy/RenderScaleRuntime";

    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)) return bytes;

        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);

        FieldNode minecraftField = findMinecraftField(node);
        MethodNode renderWorld = findRenderWorld(node);
        if (minecraftField == null || renderWorld == null) {
            System.err.println("[RenderScale Legacy] EntityRenderer layout was not recognized; "
                    + "render scaling is disabled to avoid corrupting the class.");
            return bytes;
        }

        InsnList begin = new InsnList();
        appendMinecraftReference(begin, node.name, minecraftField);
        begin.add(new MethodInsnNode(Opcodes.INVOKESTATIC, RUNTIME,
                "beginWorld", "(Ljava/lang/Object;)V", false));
        renderWorld.instructions.insert(begin);

        int exits = 0;
        for (AbstractInsnNode instruction = renderWorld.instructions.getFirst();
                instruction != null; instruction = instruction.getNext()) {
            if (instruction.getOpcode() != Opcodes.RETURN) continue;
            InsnList end = new InsnList();
            appendMinecraftReference(end, node.name, minecraftField);
            end.add(new MethodInsnNode(Opcodes.INVOKESTATIC, RUNTIME,
                    "endWorld", "(Ljava/lang/Object;)V", false));
            renderWorld.instructions.insertBefore(instruction, end);
            exits++;
        }

        if (exits == 0) {
            System.err.println("[RenderScale Legacy] renderWorld had no normal exit; "
                    + "render scaling is disabled.");
            return bytes;
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        System.out.println("[RenderScale Legacy] Installed renderWorld target swap ("
                + exits + " exits).");
        return writer.toByteArray();
    }

    private static void appendMinecraftReference(InsnList list, String owner,
            FieldNode minecraftField) {
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new FieldInsnNode(Opcodes.GETFIELD, owner,
                minecraftField.name, minecraftField.desc));
    }

    private static FieldNode findMinecraftField(ClassNode node) {
        FieldNode found = null;
        for (Object value : node.fields) {
            FieldNode field = (FieldNode) value;
            if (!"Lbao;".equals(field.desc)
                    && !"Lnet/minecraft/client/Minecraft;".equals(field.desc)) {
                continue;
            }
            if (found != null) return null;
            found = field;
        }
        return found;
    }

    private static MethodNode findRenderWorld(ClassNode node) {
        boolean obfuscated = "blt".equals(node.name);
        MethodNode found = null;
        for (Object value : node.methods) {
            MethodNode method = (MethodNode) value;
            if (!"(FJ)V".equals(method.desc)) continue;
            boolean nameMatches = (obfuscated && "a".equals(method.name))
                    || "func_78471_a".equals(method.name)
                    || "renderWorld".equals(method.name);
            if (!nameMatches) continue;
            if (found != null) return null;
            found = method;
        }
        return found;
    }
}
