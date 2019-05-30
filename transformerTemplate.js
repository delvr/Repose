function initializeCoreMod() {
	return {
		"$transformerName": {
			"target": {
				"type": "CLASS",
				"names": function(target) {
				    return [$transformedClasses];
				}
			},
			"transformer": function(classNode) {
			    var debug = true;
                var Type     = Java.type("org.objectweb.asm.Type");
                var Opcodes  = Java.type("org.objectweb.asm.Opcodes");
                var InsnNode = Java.type("org.objectweb.asm.tree.AbstractInsnNode");
			    var replacementClass         = "$replacementClass";
			    var replacedMethodOwner      = "$replacedMethodOwner";
			    var replacedMethodDevName    = "$replacedMethodDevName";
			    var replacedMethodSrgName    = "$replacedMethodSrgName";
			    var replacedMethodDescriptor = "$replacedMethodDescriptor";
                for(var iMethodNode in classNode.methods) {
                    var methodNode = classNode.methods[iMethodNode];
                    var instructions = methodNode.instructions.toArray();
                    for(var iInstruction in instructions) {
                        var instruction = instructions[iInstruction];
                        if(instruction.getType() === InsnNode.METHOD_INSN) { // method call
                            if(instruction.getOpcode() === Opcodes.INVOKESPECIAL && instruction.owner !== classNode.name) // super() call
                                continue;
                            if(instruction.owner.contains(replacedMethodOwner.replaceAll("\\\\.", "/")) &&
                              (instruction.name === replacedMethodDevName || instruction.name === replacedMethodSrgName) &&
                               instruction.desc.contains(replacedMethodDescriptor.replaceAll("\\\\.", "/"))) {
                                if(debug)
                                    print(classNode.name + " " + methodNode.name + ": redirecting call to " + replacedMethodDevName + " from " + instruction.owner.replaceAll("/", ".") + " to " + replacementClass);
                                if(instruction.getOpcode() !== Opcodes.INVOKESTATIC) {
                                    instruction.setOpcode(Opcodes.INVOKESTATIC);
                                    instruction.desc = "(" + Type.getObjectType(instruction.owner).getDescriptor() + instruction.desc.substring(1);
                                }
                                instruction.owner = replacementClass.replaceAll("\\\\.", "/");
                                instruction.name = replacedMethodDevName;
                                instruction.itf = false;
                            }
                        }
                    }
                }
				return classNode;
			}
		}
	}
}
