import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;

import dev.zelo.renderscale.legacy.EntityRendererTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class TransformerVerifier {
    public static void main(String[] args) throws Exception {
        byte[] original = read(new File(args[0]));
        byte[] transformed = new EntityRendererTransformer().transform(
                "blt", "net.minecraft.client.renderer.EntityRenderer", original);
        if (transformed == original) throw new AssertionError("Transformer made no change");

        ClassNode node = new ClassNode();
        new ClassReader(transformed).accept(node, 0);
        int begin = 0;
        int end = 0;
        for (Object methodValue : node.methods) {
            MethodNode method = (MethodNode) methodValue;
            for (AbstractInsnNode instruction = method.instructions.getFirst();
                    instruction != null; instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode)
                        || instruction.getOpcode() != Opcodes.INVOKESTATIC) continue;
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (!"dev/zelo/renderscale/legacy/RenderScaleRuntime".equals(call.owner)) {
                    continue;
                }
                if ("beginWorld".equals(call.name)) begin++;
                if ("endWorld".equals(call.name)) end++;
            }
        }
        if (begin != 1 || end < 1) {
            throw new AssertionError("Unexpected hooks: begin=" + begin + ", end=" + end);
        }
        System.out.println("Transformer verification passed: begin=" + begin
                + ", end=" + end + ", bytes=" + transformed.length);
    }

    private static byte[] read(File file) throws Exception {
        FileInputStream input = new FileInputStream(file);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
        input.close();
        return output.toByteArray();
    }
}
