package com.yelle233.liubai;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Checks actual Minecraft bytecode without opening a window or initializing the client singleton. */
final class RenderHookSelfTest {
    static void run() throws IOException {
        for (String mixin : List.of("GameRendererMixin", "EntityRenderDispatcherMixin", "EntityRendererMixin",
                "BlockEntityRenderDispatcherMixin", "ParticleEngineMixin")) {
            checkMixinTargets(mixin);
        }

        ClassNode minecraft = read("net/minecraft/client/Minecraft");
        boolean checkedOrder = false;
        for (MethodNode method : minecraft.methods) {
            List<MethodInsnNode> calls = calls(method);
            int extract = indexOf(calls, "net/minecraft/client/renderer/GameRenderer", "extract");
            int pre = indexOf(calls, "net/neoforged/neoforge/client/ClientHooks", "fireRenderFramePre");
            if (extract >= 0 && pre >= 0) {
                assert extract < pre : "Recheck the frame lifecycle against this Minecraft version";
                checkedOrder = true;
            }
        }
        assert checkedOrder : "Must verify Minecraft's actual extraction/render-event order";

        ClassNode gameMixin = read("com/yelle233/liubai/mixin/GameRendererMixin");
        assert gameMixin.methods.stream().anyMatch(method ->
                indexOf(calls(method), "com/yelle233/liubai/client/LiubaiClientSystem", "beginFrame") >= 0)
                : "The frame must start before extracting render decisions";
        ClassNode events = read("com/yelle233/liubai/client/ClientEvents");
        assert events.methods.stream().noneMatch(method ->
                indexOf(calls(method), "com/yelle233/liubai/client/LiubaiClientSystem", "beginFrame") >= 0)
                : "Late render events must not reset extraction state";
        assert events.methods.stream().anyMatch(method ->
                indexOf(calls(method), "com/yelle233/liubai/client/LiubaiClientSystem", "endFrame") >= 0)
                : "The completed extraction/render frame must publish its counters";
    }

    private static void checkMixinTargets(String name) throws IOException {
        ClassNode mixin = read("com/yelle233/liubai/mixin/" + name);
        AnnotationNode declaration = annotation(mixin.invisibleAnnotations, "Lorg/spongepowered/asm/mixin/Mixin;");
        assert declaration != null : "Missing Mixin annotation: " + name;
        List<?> targets = (List<?>) value(declaration, "value");
        ClassNode target = read(((Type) targets.getFirst()).getInternalName());
        for (MethodNode handler : mixin.methods) {
            AnnotationNode inject = annotation(handler.visibleAnnotations, "Lorg/spongepowered/asm/mixin/injection/Inject;");
            if (inject == null) inject = annotation(handler.invisibleAnnotations, "Lorg/spongepowered/asm/mixin/injection/Inject;");
            if (inject == null) continue;
            Type[] handlerArgs = Type.getArgumentTypes(handler.desc);
            for (Object selectorValue : (List<?>) value(inject, "method")) {
                String selector = (String) selectorValue;
                List<MethodNode> matches = target.methods.stream().filter(method ->
                        (selector.equals(method.name) || selector.equals(method.name + method.desc))
                                && sameArguments(method, handlerArgs)).toList();
                assert matches.size() == 1 : name + "." + handler.name + " does not match a unique target: " + selector;
                for (Object atValue : (List<?>) value(inject, "at")) {
                    AnnotationNode at = (AnnotationNode) atValue;
                    if (!"INVOKE".equals(value(at, "value"))) continue;
                    String invocation = (String) value(at, "target");
                    assert calls(matches.getFirst()).stream().anyMatch(call ->
                            invocation.equals("L" + call.owner + ";" + call.name + call.desc))
                            : "Missing injection point: " + invocation;
                }
            }
        }
    }

    private static boolean sameArguments(MethodNode method, Type[] handlerArgs) {
        Type[] args = Type.getArgumentTypes(method.desc);
        if (handlerArgs.length != args.length + 1) return false;
        for (int i = 0; i < args.length; i++) if (!args[i].equals(handlerArgs[i])) return false;
        return true;
    }

    private static ClassNode read(String name) throws IOException {
        try (var stream = RenderHookSelfTest.class.getResourceAsStream("/" + name + ".class")) {
            if (stream == null) throw new IOException("Missing bytecode: " + name);
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    private static List<MethodInsnNode> calls(MethodNode method) {
        List<MethodInsnNode> calls = new ArrayList<>();
        for (var instruction : method.instructions) if (instruction instanceof MethodInsnNode call) calls.add(call);
        return calls;
    }

    private static int indexOf(List<MethodInsnNode> calls, String owner, String name) {
        for (int i = 0; i < calls.size(); i++) {
            if (calls.get(i).owner.equals(owner) && calls.get(i).name.equals(name)) return i;
        }
        return -1;
    }

    private static AnnotationNode annotation(List<AnnotationNode> annotations, String descriptor) {
        if (annotations == null) return null;
        return annotations.stream().filter(annotation -> annotation.desc.equals(descriptor)).findFirst().orElse(null);
    }

    private static Object value(AnnotationNode annotation, String key) {
        if (annotation.values != null) {
            for (int i = 0; i < annotation.values.size(); i += 2) {
                if (key.equals(annotation.values.get(i))) return annotation.values.get(i + 1);
            }
        }
        return null;
    }
}
