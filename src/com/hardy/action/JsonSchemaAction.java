package com.hardy.action;

import com.google.gson.GsonBuilder;
import com.hardy.model.NormalTypeConst;
import com.hardy.model.Schema;
import com.hardy.model.SchemaType;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NonNls;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: Hardy
 * Date:   2019/2/22
 * Description:
 **/
public class JsonSchemaAction extends AnAction {

    private static NotificationGroup notificationGroup = new NotificationGroup(
            "JsonTool.NotificationGroup", NotificationDisplayType.BALLOON, true);


    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(CommonDataKeys.PROJECT);
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        // 获取鼠标当前偏移
        PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());

        // 获取操作类
        PsiClass selectedClass = PsiTreeUtil.getContextOfType(element, PsiClass.class);

        // 结果收集保证有序
        try {
            Schema schema = classParser(selectedClass);
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(schema);
            StringSelection selection = new StringSelection(json);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
            String message = "Convert " + selectedClass.getName() + " to JSON success, copied to clipboard.";
            Notification success = notificationGroup.createNotification(message, NotificationType.INFORMATION);
            Notifications.Bus.notify(success, project);
        } catch (Exception ex) {
            Notification error = notificationGroup.createNotification("Convert to JSON failed.", NotificationType.ERROR);
            Notifications.Bus.notify(error, project);
        }
    }


    private Schema classParser(PsiClass clazz) {
        Schema schema = Schema.createObject();

        PsiClass superClass = clazz.getSuperClass();
        if (superClass != null && !"Object".equals(superClass.getName())) {
            getFields(superClass, schema);
        }

        getFields(clazz, schema);

        return schema;
    }

    private void getFields(PsiClass clazz, Schema schema) {
        for (PsiField field : clazz.getFields()) {
            Schema fieldSchema = getField(field.getTypeElement());

            // 获取注释 //** . */
            if (field.getDocComment() != null && field.getDocComment().getText() != null) {
                fieldSchema.setDescription(field.getDocComment().getText().replaceAll("[/*]", "").trim());
            }
            // 获取 // 注释
            else {
                StringBuilder sb = new StringBuilder();
                for (String s : field.getText().split("\n")) {
                    String temp = s.trim();
                    if (temp.startsWith("//")) {
                        sb.append(temp.replaceAll("//*", "").trim());
                        sb.append(" ");
                    }
                }
                fieldSchema.setDescription(sb.toString());
            }

            schema.getProperties().set(field.getName(), fieldSchema);
        }
    }

    private Schema getField(PsiTypeElement element) {
        // 获取字段类型
        PsiType type = element.getType();
        String typeName = type.getPresentableText();

        // 如果是Object
        if ("Object".equals(typeName)) {
            return Schema.createObject();
        }
        // 基本类型
        else if (type instanceof PsiPrimitiveType) {
            return create(typeName);
        }
        // 数组
        else if (type instanceof PsiArrayType) {
            PsiTypeElement arrayElement = (PsiTypeElement) element.getFirstChild();
            return Schema.createArray(getField(arrayElement));
        }
        // 列表
        else if (typeName.split("<")[0].contains("List") || typeName.split("<")[0].equals("Collection")) {
            PsiJavaCodeReferenceElement referenceElement = element.getInnermostComponentReferenceElement();
            PsiReferenceParameterList referenceParameter = referenceElement.getParameterList();
            PsiTypeElement[] generics = referenceParameter.getTypeParameterElements();

            // 不存在泛型
            if (generics.length == 0) return Schema.createArray(Schema.createObject());

            // 获取泛型
            PsiTypeElement generic = generics[0];

            return Schema.createArray(getField(generic));
        }
        // map
        else if (typeName.split("<")[0].contains("Map")) {
            PsiJavaCodeReferenceElement referenceElement = element.getInnermostComponentReferenceElement();
            PsiReferenceParameterList referenceParameter = referenceElement.getParameterList();
            PsiTypeElement[] generics = referenceParameter.getTypeParameterElements();

            return Schema.createObject();
        }
        // 其他类型
        else if (NormalTypeConst.isNormalType(typeName)) {
            return create(typeName);
        }
        // 其他的类
        else {
            PsiClass clazz = PsiUtil.resolveClassInType(element.getType());
            return classParser(clazz);
        }
    }

    private Schema create(String typeName) {
        String name = typeName.toLowerCase();

        if ("boolean".equals(name))
            return new Schema(SchemaType.BOOLEAN);
        else if (Arrays.asList("char", "string", "bigdecimal", "date").contains(name))
            return new Schema(SchemaType.STRING);
        else if (Arrays.asList("byte", "short", "int", "long", "integer").contains(name))
            return new Schema(SchemaType.INTEGER);
        else
            return new Schema(SchemaType.NUMBER);
    }
}