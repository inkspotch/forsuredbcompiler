package com.forsuredb.annotationprocessor.generator.code;

import com.forsuredb.annotation.FSTable;
import com.forsuredb.annotationprocessor.info.ColumnInfo;
import com.forsuredb.annotationprocessor.info.TableInfo;
import com.forsuredb.api.FSGetApi;
import com.forsuredb.api.FSTableCreator;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static com.forsuredb.annotationprocessor.generator.code.JavadocInfo.inlineClassLink;

public class TableCreatorGenerator extends JavaSourceGenerator {

    private final Collection<TableInfo> tables;

    public TableCreatorGenerator(ProcessingEnvironment processingEnv, String appPackageName, Collection<TableInfo> tables) {
        super(processingEnv, appPackageName + "." + "TableGenerator");
        this.tables = tables;
    }

    @Override
    protected String getCode() {
        JavadocInfo javadoc = JavadocInfo.builder()
                .startParagraph()
                .addLine("This is an automatically-generated class. DO NOT modify it!")
                .endParagraph()
                .startParagraph()
                .addLine("$L generates the basic information about the tables", getOutputClassName(false))
                .addLine("you have described in your extensions of")
                .addLine("$L. It should be called as you", inlineClassLink(FSGetApi.class))
                .addLine("initialize the database.")
                .endParagraph()
                .startParagraph()
                .addLine("You have two options when you generate the tables. First, you can accept")
                .addLine("the default authority (\"com.forsuredb.testapp.content\") as below:")
                .startCode()
                .addLine("TableGenerator.generate();")
                .endCode()
                .addLine("Or you can generate the tables, specifying your authority, as below:")
                .startCode()
                .addLine("TableGenerator.generate(\"my.apps.content.authority\");")
                .endCode()
                .endParagraph()
                .addLine(JavadocInfo.AUTHOR_STRING)
                .addLine()
                .build();
        return JavaFile.builder(getOutputPackageName(), TypeSpec.classBuilder(getOutputClassName(false))
                        .addModifiers(Modifier.PUBLIC)
                        .addJavadoc(javadoc.stringToFormat(), javadoc.replacements())
                        .addMethod(noArgGenerateMethod())
                        .addMethod(generateMethodWithAuthorityArg())
                        .build())
                .indent(JAVA_INDENT)
                .build()
                .toString();
    }

    private MethodSpec generateMethodWithAuthorityArg() {
        JavadocInfo jd = JavadocInfo.builder()
                .startParagraph()
                .addLine("Creates a list of $L", inlineClassLink(FSTableCreator.class))
                .addLine("objects that tell the underlying routing mechanism all it needs to")
                .addLine("know in order to set up the appropriate routes to the tables.")
                .endParagraph()
                .addLine("@param authority The authority for your database resources")
                .addLine("@return A list of $L corresponding", inlineClassLink(FSTableCreator.class))
                .addLine("to all $L extensions annotated with", inlineClassLink(FSGetApi.class))
                .addLine(inlineClassLink(FSTable.class))
                .addLine("@see #generate()")
                .addLine()
                .build();
        MethodSpec.Builder codeBuilder = MethodSpec.methodBuilder("generate")
                .addJavadoc(jd.stringToFormat(), jd.replacements())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(List.class, FSTableCreator.class))
                .addParameter(String.class, "authority")
                .addCode(CodeBlock.builder().add("// provide a reasonable default authority\n")
                        .addStatement("authority = authority == null || authority.isEmpty() ? $S : authority", "com.forsuredb.testapp.content")
                        .addStatement("final $T retList = new $T()", ParameterizedTypeName.get(List.class, FSTableCreator.class), ParameterizedTypeName.get(LinkedList.class, FSTableCreator.class))
                        .build());
        for (TableInfo table : TableDataUtil.tablesSortedByName(tables)) {
            codeBuilder.addStatement(createAddFSTableCreatorLine(table));
        }
        return codeBuilder.addStatement("return retList").build();
    }

    private MethodSpec noArgGenerateMethod() {
        JavadocInfo jd = JavadocInfo.builder()
                .startParagraph()
                .addLine("Creates a list of $L", inlineClassLink(FSTableCreator.class))
                .addLine("objects that tell the underlying routing mechanism all it needs to")
                .addLine("know in order to set up the appropriate routes to the tables.")
                .endParagraph()
                .startParagraph()
                .addLine("This version will use the default authority,")
                .addLine("$S", "com.forsuredb.testapp.content")
                .endParagraph()
                .addLine("@return A list of $L corresponding", inlineClassLink(FSTableCreator.class))
                .addLine("to all $L extensions annotated with", inlineClassLink(FSGetApi.class))
                .addLine(inlineClassLink(FSTable.class))
                .addLine("@see #generate(String)")
                .addLine()
                .build();
        return MethodSpec.methodBuilder("generate")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(List.class, FSTableCreator.class))
                .addJavadoc(jd.stringToFormat(), jd.replacements())
                .addCode(CodeBlock.builder()
                        .addStatement("return generate(null)")
                        .build())
                .build();
    }

    private String createAddFSTableCreatorLine(TableInfo tableInfo) {
        StringBuffer buf = new StringBuffer("retList").append(".add(new FSTableCreator(")
                .append("authority, ")
                .append(tableInfo.getQualifiedClassName()).append(".class");

        if (tableInfo.hasStaticData()) {
            buf.append(", \"").append(tableInfo.getStaticDataAsset()).append("\"")
                    .append(", \"").append(tableInfo.getStaticDataRecordName()).append("\"");
        }

        for (ColumnInfo column : tableInfo.getForeignKeyColumns()) {
            buf.append(", ").append(column.getForeignKeyInfo().getApiClassName()).append(".class");
        }

        return buf.append("))").toString();
    }
}
