package io.github.moulberry.hudcaching.asm.transformers;

import io.github.moulberry.hudcaching.asm.ITransformer;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import static org.objectweb.asm.Opcodes.*;

public class GuiIngameForgeTransformer implements ITransformer {

    @Override
    public String[] getClassName() {
        return new String[]{"net.minecraftforge.client.GuiIngameForge"};
    }

    @Override
    public void transform(ClassNode classNode, String name) {
        classNode.interfaces.add("io/github/moulberry/hudcaching/asm/interfaces/IGuiIngameForgeListener");

        for (MethodNode method : classNode.methods) {
            switch(method.name) {
                case "pre":
                case "post":
                case "renderCrosshairs":
                    method.access = ACC_PUBLIC;
            }
        }
    }

}
