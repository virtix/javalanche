/*
* Copyright (C) 2011 Saarland University
* 
* This file is part of Javalanche.
* 
* Javalanche is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* Javalanche is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Lesser Public License for more details.
* 
* You should have received a copy of the GNU Lesser Public License
* along with Javalanche.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.unisb.cs.st.javalanche.mutation.bytecodeMutations.negateJumps;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import de.unisb.cs.st.javalanche.mutation.results.persistence.MutationManager;

public class NegateJumpsClassAdapter extends ClassAdapter {

	
	private String className;
	private Map<Integer, Integer> possibilities = new HashMap<Integer, Integer>();
	private final MutationManager mm;
	
	public NegateJumpsClassAdapter(ClassVisitor cv) {
		super(cv);
		mm = new MutationManager();// TODO
	}

	@Override
	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {
		className = name;
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		// String lastMethodName = name;
		MethodVisitor superVisitor = super.visitMethod(access, name, desc,
				signature, exceptions);
		MethodVisitor actualAdapter = new NegateJumpsMethodAdapter(
				superVisitor, className, name, possibilities, mm, desc);
		return actualAdapter;
	}

}
