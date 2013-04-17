package net.osmand.translator.utils;

import java.io.PrintStream;
import java.util.List;

import net.osmand.translator.visitor.MethodVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.google.devtools.j2objc.gen.ObjectiveCHeaderGenerator;

public class MethodHandler extends AbstractHandler{

	public static void printMethodsInfo(CompilationUnit parse, PrintStream out) {
		MethodVisitor mVisitor = new MethodVisitor();
		parse.accept(mVisitor);
		for (MethodDeclaration method : mVisitor.getMethods()) {
			StringBuffer buffer = new StringBuffer();
//	    	modifiers
	    	applyModifiers(method.getModifiers(), buffer);
	    	Type returnType = method.getReturnType2();
	    	if (returnType.toString().equals("void")) {
	    		buffer.append("void ");
	    	} else {
	    		applyType(returnType, buffer);	    		
	    	}
	    	buffer.append(method.getName());
	    	buffer.append("(");
	    	applyParameters(method, buffer);
	    	buffer.append(")");
	    	buffer.append("{");
	    	fillBody(method, buffer);
	    	buffer.append("}");
	    	out.println(buffer);
	    	out.println();
		}
	}

	
	private static void applyParameters(MethodDeclaration method, StringBuffer buffer) {
		List parameters = method.parameters();
		for (Object p : parameters) {
			ASTNode node = (ASTNode)p;
			buffer.append(node.toString() + ", ");
		}
		buffer.deleteCharAt(buffer.length()-2);
	} 
	
	private static void fillBody(MethodDeclaration method, StringBuffer buffer) {
		
		Block body = method.getBody();
		buffer.append(body);
	}
	
	
	
	private void parseSimpleForStatement() {

	}
	
}
