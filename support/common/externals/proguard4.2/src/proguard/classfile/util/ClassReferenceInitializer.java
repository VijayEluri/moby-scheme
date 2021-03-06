/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2007 Eric Lafortune (eric@graphics.cornell.edu)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package proguard.classfile.util;

import proguard.classfile.*;
import proguard.classfile.attribute.*;
import proguard.classfile.attribute.annotation.*;
import proguard.classfile.attribute.annotation.visitor.*;
import proguard.classfile.attribute.visitor.*;
import proguard.classfile.constant.*;
import proguard.classfile.constant.visitor.ConstantVisitor;
import proguard.classfile.visitor.*;


/**
 * This ClassVisitor initializes the references of all classes that
 * it visits.
 * <p>
 * All class constant pool entries get direct references to the corresponding
 * classes. These references make it more convenient to travel up and across
 * the class hierarchy.
 * <p>
 * All field and method reference constant pool entries get direct references
 * to the corresponding classes, fields, and methods.
 * <p>
 * All name and type constant pool entries get a list of direct references to
 * the classes listed in the type.
 * <p>
 * This visitor optionally prints warnings if some items can't be found.
 * <p>
 * The class hierarchy must be initialized before using this visitor.
 *
 * @author Eric Lafortune
 */
public class ClassReferenceInitializer
extends      SimplifiedVisitor
implements   ClassVisitor,
             MemberVisitor,
             ConstantVisitor,
             AttributeVisitor,
             LocalVariableInfoVisitor,
             LocalVariableTypeInfoVisitor,
             AnnotationVisitor,
             ElementValueVisitor
{
    private final ClassPool      programClassPool;
    private final ClassPool      libraryClassPool;
    private final WarningPrinter warningPrinter;

    private final MemberFinder memberFinder = new MemberFinder();


    /**
     * Creates a new ClassReferenceInitializer that initializes the references
     * of all visited class files, optionally printing warnings if some classes
     * can't be found.
     */
    public ClassReferenceInitializer(ClassPool      programClassPool,
                                     ClassPool      libraryClassPool,
                                     WarningPrinter warningPrinter)
    {
        this.programClassPool = programClassPool;
        this.libraryClassPool = libraryClassPool;
        this.warningPrinter   = warningPrinter;
    }


    // Implementations for ClassVisitor.

    public void visitProgramClass(ProgramClass programClass)
    {
        // Initialize the constant pool entries.
        programClass.constantPoolEntriesAccept(this);

        // Initialize all fields and methods.
        programClass.fieldsAccept(this);
        programClass.methodsAccept(this);

        // Initialize the attributes.
        programClass.attributesAccept(this);
    }


    public void visitLibraryClass(LibraryClass libraryClass)
    {
        // Initialize all fields and methods.
        libraryClass.fieldsAccept(this);
        libraryClass.methodsAccept(this);
    }


    // Implementations for MemberVisitor.

    public void visitProgramField(ProgramClass programClass, ProgramField programField)
    {
        programField.referencedClass =
            findReferencedClass(programField.getDescriptor(programClass));

        // Initialize the attributes.
        programField.attributesAccept(programClass, this);
    }


    public void visitProgramMethod(ProgramClass programClass, ProgramMethod programMethod)
    {
        programMethod.referencedClasses =
            findReferencedClasses(programMethod.getDescriptor(programClass));

        // Initialize the attributes.
        programMethod.attributesAccept(programClass, this);
    }


    public void visitLibraryField(LibraryClass libraryClass, LibraryField libraryField)
    {
        libraryField.referencedClass =
            findReferencedClass(libraryField.getDescriptor(libraryClass));
    }


    public void visitLibraryMethod(LibraryClass libraryClass, LibraryMethod libraryMethod)
    {
        libraryMethod.referencedClasses =
            findReferencedClasses(libraryMethod.getDescriptor(libraryClass));
    }


    // Implementations for ConstantVisitor.

    public void visitAnyConstant(Clazz clazz, Constant constant) {}


    public void visitStringConstant(Clazz clazz, StringConstant stringConstant)
    {
        // Fill out the String class.
        stringConstant.javaLangStringClass =
            findClass(ClassConstants.INTERNAL_NAME_JAVA_LANG_STRING);
    }


    public void visitAnyRefConstant(Clazz clazz, RefConstant refConstant)
    {
        String className = refConstant.getClassName(clazz);

        // See if we can find the referenced class.
        // Unresolved references are assumed to refer to library classes
        // that will not change anyway.
        Clazz referencedClass = findClass(className);

        if (referencedClass != null &&
            !ClassUtil.isInternalArrayType(className))
        {
            String name = refConstant.getName(clazz);
            String type = refConstant.getType(clazz);

            boolean isFieldRef = refConstant.getTag() == ClassConstants.CONSTANT_Fieldref;

            // See if we can find the referenced class member somewhere in the
            // hierarchy.
            refConstant.referencedMember = memberFinder.findMember(clazz,
                                                                   referencedClass,
                                                                   name,
                                                                   type,
                                                                   isFieldRef);
            refConstant.referencedClass  = memberFinder.correspondingClass();

            if (refConstant.referencedMember == null &&
                warningPrinter != null)
            {
                // We've haven't found the class member anywhere in the hierarchy.
                warningPrinter.print("Warning: " +
                                     ClassUtil.externalClassName(clazz.getName()) +
                                     ": can't find referenced " +
                                     (isFieldRef ?
                                         "field '"  + ClassUtil.externalFullFieldDescription(0, name, type) :
                                         "method '" + ClassUtil.externalFullMethodDescription(className, 0, name, type)) +
                                     "' in class " +
                                     ClassUtil.externalClassName(className));
            }
        }
    }


    public void visitClassConstant(Clazz clazz, ClassConstant classConstant)
    {
        // Fill out the referenced class.
        classConstant.referencedClass =
            findClass(classConstant.getName(clazz));

        // Fill out the Class class.
        classConstant.javaLangClassClass =
            findClass(ClassConstants.INTERNAL_NAME_JAVA_LANG_CLASS);
    }


    // Implementations for AttributeVisitor.

    public void visitAnyAttribute(Clazz clazz, Attribute attribute) {}


    public void visitEnclosingMethodAttribute(Clazz clazz, EnclosingMethodAttribute enclosingMethodAttribute)
    {
        String className = enclosingMethodAttribute.getClassName(clazz);

        // See if we can find the referenced class.
        Clazz referencedClass = findClass(className);

        if (referencedClass == null)
        {
            // We couldn't find the enclosing class.
            if (warningPrinter != null)
            {
                warningPrinter.print("Warning: " +
                                     ClassUtil.externalClassName(clazz.getName()) +
                                     ": can't find enclosing class " +
                                     ClassUtil.externalClassName(className));
            }

            return;
        }

        // Make sure there is actually an enclosed method.
        if (enclosingMethodAttribute.u2nameAndTypeIndex == 0)
        {
            return;
        }

        String name = enclosingMethodAttribute.getName(clazz);
        String type = enclosingMethodAttribute.getType(clazz);

        // See if we can find the method in the referenced class.
        Method referencedMethod = referencedClass.findMethod(name, type);

        if (referencedMethod == null)
        {
            // We couldn't find the enclosing method.
            if (warningPrinter != null)
            {
                warningPrinter.print("Warning: " +
                                     ClassUtil.externalClassName(clazz.getName()) +
                                     ": can't find enclosing method '" +
                                     ClassUtil.externalFullMethodDescription(className, 0, name, type) +
                                     "' in class " +
                                     ClassUtil.externalClassName(className));
            }

            return;
        }

        // Save the references.
        enclosingMethodAttribute.referencedClass  = referencedClass;
        enclosingMethodAttribute.referencedMethod = referencedMethod;
    }


    public void visitCodeAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute)
    {
        // Initialize the nested attributes.
        codeAttribute.attributesAccept(clazz, method, this);
    }


    public void visitLocalVariableTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTableAttribute localVariableTableAttribute)
    {
        // Initialize the local variables.
        localVariableTableAttribute.localVariablesAccept(clazz, method, codeAttribute, this);
    }


    public void visitLocalVariableTypeTableAttribute(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTypeTableAttribute localVariableTypeTableAttribute)
    {
        // Initialize the local variable types.
        localVariableTypeTableAttribute.localVariablesAccept(clazz, method, codeAttribute, this);
    }


    public void visitSignatureAttribute(Clazz clazz, SignatureAttribute signatureAttribute)
    {
        signatureAttribute.referencedClasses =
            findReferencedClasses(clazz.getString(signatureAttribute.u2signatureIndex));
    }


    public void visitAnyAnnotationsAttribute(Clazz clazz, AnnotationsAttribute annotationsAttribute)
    {
        // Initialize the annotations.
        annotationsAttribute.annotationsAccept(clazz, this);
    }


    public void visitAnyParameterAnnotationsAttribute(Clazz clazz, Method method, ParameterAnnotationsAttribute parameterAnnotationsAttribute)
    {
        // Initialize the annotations.
        parameterAnnotationsAttribute.annotationsAccept(clazz, method, this);
    }


    public void visitAnnotationDefaultAttribute(Clazz clazz, Method method, AnnotationDefaultAttribute annotationDefaultAttribute)
    {
        // Initialize the annotation.
        annotationDefaultAttribute.defaultValueAccept(clazz, this);
    }


    // Implementations for LocalVariableInfoVisitor.

    public void visitLocalVariableInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableInfo localVariableInfo)
    {
        localVariableInfo.referencedClass =
            findReferencedClass(clazz.getString(localVariableInfo.u2descriptorIndex));
    }


    // Implementations for LocalVariableTypeInfoVisitor.

    public void visitLocalVariableTypeInfo(Clazz clazz, Method method, CodeAttribute codeAttribute, LocalVariableTypeInfo localVariableTypeInfo)
    {
        localVariableTypeInfo.referencedClasses =
            findReferencedClasses(clazz.getString(localVariableTypeInfo.u2signatureIndex));
    }


    // Implementations for AnnotationVisitor.

    public void visitAnnotation(Clazz clazz, Annotation annotation)
    {
        annotation.referencedClasses =
            findReferencedClasses(clazz.getString(annotation.u2typeIndex));

        // Initialize the element values.
        annotation.elementValuesAccept(clazz, this);
    }


    // Implementations for ElementValueVisitor.

    public void visitConstantElementValue(Clazz clazz, Annotation annotation, ConstantElementValue constantElementValue)
    {
        initializeElementValue(clazz, annotation, constantElementValue);
    }


    public void visitEnumConstantElementValue(Clazz clazz, Annotation annotation, EnumConstantElementValue enumConstantElementValue)
    {
        initializeElementValue(clazz, annotation, enumConstantElementValue);

        enumConstantElementValue.referencedClasses =
            findReferencedClasses(clazz.getString(enumConstantElementValue.u2typeNameIndex));
    }


    public void visitClassElementValue(Clazz clazz, Annotation annotation, ClassElementValue classElementValue)
    {
        initializeElementValue(clazz, annotation, classElementValue);

        classElementValue.referencedClasses =
            findReferencedClasses(clazz.getString(classElementValue.u2classInfoIndex));
    }


    public void visitAnnotationElementValue(Clazz clazz, Annotation annotation, AnnotationElementValue annotationElementValue)
    {
        initializeElementValue(clazz, annotation, annotationElementValue);

        // Initialize the annotation.
        annotationElementValue.annotationAccept(clazz, this);
    }


    public void visitArrayElementValue(Clazz clazz, Annotation annotation, ArrayElementValue arrayElementValue)
    {
        initializeElementValue(clazz, annotation, arrayElementValue);

        // Initialize the element values.
        arrayElementValue.elementValuesAccept(clazz, annotation, this);
    }


    /**
     * Initializes the referenced method of an element value, if any.
     */
    private void initializeElementValue(Clazz clazz, Annotation annotation, ElementValue elementValue)
    {
        // See if we have a referenced class.
        if (annotation                      != null &&
            annotation.referencedClasses    != null &&
            elementValue.u2elementNameIndex != 0)
        {
            // See if we can find the method in the referenced class
            // (ignoring the descriptor).
            String name = clazz.getString(elementValue.u2elementNameIndex);

            Clazz referencedClass = annotation.referencedClasses[0];
            elementValue.referencedClass  = referencedClass;
            elementValue.referencedMethod = referencedClass.findMethod(name, null);
        }
    }


    // Small utility methods.

    /**
     * Returns the single class referenced by the given descriptor, or
     * <code>null</code> if there isn't any useful reference.
     */
    private Clazz findReferencedClass(String descriptor)
    {
        DescriptorClassEnumeration enumeration =
            new DescriptorClassEnumeration(descriptor);

        enumeration.nextFluff();

        if (enumeration.hasMoreClassNames())
        {
            return findClass(enumeration.nextClassName());
        }

        return null;
    }


    /**
     * Returns an array of classes referenced by the given descriptor, or
     * <code>null</code> if there aren't any useful references.
     */
    private Clazz[] findReferencedClasses(String descriptor)
    {
        DescriptorClassEnumeration enumeration =
            new DescriptorClassEnumeration(descriptor);

        int classCount = enumeration.classCount();
        if (classCount > 0)
        {
            Clazz[] referencedClasses = new Clazz[classCount];

            boolean foundReferencedClasses = false;

            for (int index = 0; index < classCount; index++)
            {
                String fluff = enumeration.nextFluff();
                String name  = enumeration.nextClassName();

                Clazz referencedClass = findClass(name);

                if (referencedClass != null)
                {
                    referencedClasses[index] = referencedClass;
                    foundReferencedClasses = true;
                }
            }

            if (foundReferencedClasses)
            {
                return referencedClasses;
            }
        }

        return null;
    }


    /**
     * Returns the class with the given name, either for the program class pool
     * or from the library class pool, or <code>null</code> if it can't be found.
     */
    private Clazz findClass(String name)
    {
        // First look for the class in the program class pool.
        Clazz clazz = programClassPool.getClass(name);

        // Otherwise look for the class in the library class pool.
        if (clazz == null)
        {
            clazz = libraryClassPool.getClass(name);
        }

        return clazz;
    }
}
