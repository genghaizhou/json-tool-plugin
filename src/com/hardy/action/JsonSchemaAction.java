package com.hardy.action;

import com.google.gson.GsonBuilder;
import com.hardy.model.NormalTypeConst;
import com.hardy.model.Schema;
import com.hardy.model.SchemaType;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.LinkedList;


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
        Schema root = Schema.createObject();

        LinkedList<PsiClass> clazzs = new LinkedList<>();

        // 收集所有类
        do {
            clazzs.addFirst(clazz);
            clazz = clazz.getSuperClass();
        } while (clazz != null && !"Object".equals(clazz.getName()));

        // 构造schema
        clazzs.forEach(c -> getFields(c, root));
        return root;
    }

    private void getFields(PsiClass clazz, Schema root) {
        for (PsiField field : clazz.getFields()) {
            // 过滤掉静态字段
            if (field.hasModifierProperty(PsiModifier.STATIC)) return;

            PsiTypeElement element = field.getTypeElement();
            if (element == null) return;

            // 生成基础的schema
            Schema schema = genField(element);

            // 构建注释
            genComment(schema, field);

            // 构建注解
            boolean require = genAnnotation(schema, field);

            // 添加到root schema
            if (require) root.addRequire(field.getName());
            root.getProperties().set(field.getName(), schema);
        }
    }

    // 构建字段的schema
    private Schema genField(PsiTypeElement element) {
        // 获取字段类型
        PsiType type = element.getType();
        String typeName = type.getPresentableText();

        // 如果是Object
        if ("Object".equals(typeName)) {
            return Schema.createObject();
        }
        // 基本类型
        else if (type instanceof PsiPrimitiveType) {
            return Schema.createBasic(typeName);
        }
        // 数组
        else if (type instanceof PsiArrayType) {
            PsiTypeElement arrayElement = (PsiTypeElement) element.getFirstChild();
            return Schema.createArray(genField(arrayElement));
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

            return Schema.createArray(genField(generic));
        }
        // map
        else if (typeName.split("<")[0].contains("Map")) {
            PsiJavaCodeReferenceElement referenceElement = element.getInnermostComponentReferenceElement();
            PsiReferenceParameterList referenceParameter = referenceElement.getParameterList();
            PsiTypeElement[] generics = referenceParameter.getTypeParameterElements();

            return Schema.createObject();
        }
        // 正常类型
        else if (NormalTypeConst.isNormalType(typeName)) {
            return Schema.createBasic(typeName);
        }
        // 其他的类
        else {
            PsiClass clazz = PsiUtil.resolveClassInType(element.getType());
            return classParser(clazz);
        }
    }

    // 构建注释
    private void genComment(Schema schema, PsiField field) {
        // 获取注释 //** . */
        if (field.getDocComment() != null && field.getDocComment().getText() != null) {
            schema.setDescription(field.getDocComment().getText().replaceAll("[/*]", "").trim());
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
            schema.setDescription(sb.toString());
        }
    }

    // 构建注解 (是否是必须的)
    private boolean genAnnotation(Schema schema, PsiField field) {
        PsiModifierList modifierList = field.getModifierList();
        if (modifierList == null) return false;

        PsiAnnotation[] annotations = modifierList.getAnnotations();
        if (annotations.length == 0) return false;

        boolean require = false;

        // 字符串
        if (schema.getType().equals(SchemaType.STRING.val)) {
            for (PsiAnnotation annotation : annotations) {
                String qualifiedName = annotation.getQualifiedName();
                if (qualifiedName == null) continue;

                // 非处理注解
                if (!qualifiedName.contains("javax.validation.constraints")) continue;

                switch (qualifiedName) {
                    case "javax.validation.constraints.NotNull":
                        require = true;
                        break;
                    case "javax.validation.constraints.NotBlank":
                        require = true;
                        schema.setMinLength(1);
                        break;
                    case "javax.validation.constraints.Pattern":
                        String regexp = AnnotationUtil.getStringAttributeValue(annotation, "regexp");
                        if (regexp != null && !regexp.isEmpty()) schema.setPattern(regexp);
                        break;
                    case "javax.validation.constraints.Size":
                        int min = AnnotationUtil.getLongAttributeValue(annotation, "min").intValue();
                        int max = AnnotationUtil.getLongAttributeValue(annotation, "max").intValue();

                        schema.setMinLength(min);
                        schema.setMaxLength(max);
                        break;
                }
            }
        }

        // 整数
        if (schema.getType().equals(SchemaType.INTEGER.val)) {
            for (PsiAnnotation annotation : annotations) {
                String qualifiedName = annotation.getQualifiedName();
                if (qualifiedName == null) continue;

                // 非处理注解
                if (!qualifiedName.contains("javax.validation.constraints")) continue;

                switch (qualifiedName) {
                    case "javax.validation.constraints.NotNull":
                        require = true;
                        break;
                    case "javax.validation.constraints.Min":
                        int min = AnnotationUtil.getLongAttributeValue(annotation, "value").intValue();
                        schema.setMinimum(min);
                        break;
                    case "javax.validation.constraints.Max":
                        int max = AnnotationUtil.getLongAttributeValue(annotation, "value").intValue();
                        schema.setMaximum(max);
                        break;
                    case "javax.validation.constraints.Positive":
                        schema.setMinimum(0);
                        schema.setExclusiveMinimum(true);
                        break;
                    case "javax.validation.constraints.PositiveOrZero":
                        schema.setMinimum(0);
                        break;
                    case "javax.validation.constraints.Negative":
                        schema.setMaximum(0);
                        schema.setExclusiveMaximum(true);
                        break;
                    case "javax.validation.constraints.NegativeOrZero":
                        schema.setMaximum(0);
                        break;
                }
            }
        }

        // 数字
        if (schema.getType().equals(SchemaType.NUMBER.val)) {
            for (PsiAnnotation annotation : annotations) {
                String qualifiedName = annotation.getQualifiedName();
                if (qualifiedName == null) continue;

                // 非处理注解
                if (!qualifiedName.contains("javax.validation.constraints")) continue;

                switch (qualifiedName) {
                    case "javax.validation.constraints.NotNull":
                        require = true;
                        break;
                    case "javax.validation.constraints.Min":
                        double min = AnnotationUtil.getLongAttributeValue(annotation, "value").doubleValue();
                        schema.setMinimum(min);
                        break;
                    case "javax.validation.constraints.Max":
                        double max = AnnotationUtil.getLongAttributeValue(annotation, "value").doubleValue();
                        schema.setMaximum(max);
                        break;
                    case "javax.validation.constraints.DecimalMin":
                        double bmin = Double.valueOf(AnnotationUtil.getStringAttributeValue(annotation, "value"));
                        boolean bminIn = AnnotationUtil.getBooleanAttributeValue(annotation, "inclusive");

                        schema.setMinimum(bmin);
                        if (!bminIn) schema.setExclusiveMinimum(true);
                        break;
                    case "javax.validation.constraints.DecimalMax":
                        double bmax = Double.valueOf(AnnotationUtil.getStringAttributeValue(annotation, "value"));
                        boolean bmaxIn = AnnotationUtil.getBooleanAttributeValue(annotation, "inclusive");

                        schema.setMaximum(bmax);
                        if (!bmaxIn) schema.setExclusiveMaximum(true);
                        break;
                    case "javax.validation.constraints.Positive":
                        schema.setMinimum(0);
                        schema.setExclusiveMinimum(true);
                        break;
                    case "javax.validation.constraints.PositiveOrZero":
                        schema.setMinimum(0);
                        break;
                    case "javax.validation.constraints.Negative":
                        schema.setMaximum(0);
                        schema.setExclusiveMaximum(true);
                        break;
                    case "javax.validation.constraints.NegativeOrZero":
                        schema.setMaximum(0);
                        break;
                }
            }
        }
        return require;
    }
}
