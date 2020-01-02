/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scw.core.cglib.proxy;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import scw.core.asm.Type;
import scw.core.cglib.core.Block;
import scw.core.cglib.core.ClassEmitter;
import scw.core.cglib.core.CodeEmitter;
import scw.core.cglib.core.Constants;
import scw.core.cglib.core.EmitUtils;
import scw.core.cglib.core.MethodInfo;
import scw.core.cglib.core.Signature;
import scw.core.cglib.core.TypeUtils;

@SuppressWarnings({"rawtypes"})
class InvocationHandlerGenerator
implements CallbackGenerator
{
    public static final InvocationHandlerGenerator INSTANCE = new InvocationHandlerGenerator();

    private static final Type INVOCATION_HANDLER =
      TypeUtils.parseType(InvocationHandler.class.getName());
    private static final Type UNDECLARED_THROWABLE_EXCEPTION =
      TypeUtils.parseType(UndeclaredThrowableException.class.getName());
    private static final Type METHOD =
      TypeUtils.parseType(Method.class.getName());
    private static final Signature INVOKE =
      TypeUtils.parseSignature("Object invoke(Object, java.lang.reflect.Method, Object[])");

    public void generate(ClassEmitter ce, Context context, List methods) {
        for (Iterator it = methods.iterator(); it.hasNext();) {
            MethodInfo method = (MethodInfo)it.next();
            Signature impl = context.getImplSignature(method);
            ce.declare_field(Constants.PRIVATE_FINAL_STATIC, impl.getName(), METHOD, null);

            CodeEmitter e = context.beginMethod(ce, method);
            Block handler = e.begin_block();
            context.emitCallback(e, context.getIndex(method));
            e.load_this();
            e.getfield(impl.getName());
            e.create_arg_array();
            e.invoke_interface(INVOCATION_HANDLER, INVOKE);
            e.unbox(method.getSignature().getReturnType());
            e.return_value();
            handler.end();
            EmitUtils.wrap_undeclared_throwable(e, handler, method.getExceptionTypes(), UNDECLARED_THROWABLE_EXCEPTION);
            e.end_method();
        }
    }

    public void generateStatic(CodeEmitter e, Context context, List methods) {
        for (Iterator it = methods.iterator(); it.hasNext();) {
            MethodInfo method = (MethodInfo)it.next();
            EmitUtils.load_method(e, method);
            e.putfield(context.getImplSignature(method).getName());
        }
    }
}