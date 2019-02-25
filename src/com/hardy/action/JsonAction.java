package com.hardy.action;

import com.google.gson.GsonBuilder;
import com.hardy.model.NormalTypeConst;
import com.hardy.model.KV;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: Hardy
 * Date:   2019/2/22
 * Description:
 **/
public class JsonAction extends AnAction {

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
            KV kv = classParser(selectedClass);
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(kv);
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

    private KV classParser(PsiClass clazz) {
        KV kv = KV.create();
        PsiClass superClass = clazz.getSuperClass();
        if (superClass != null && !"Object".equals(superClass.getName())) {
            getFields(superClass, kv);
        }

        getFields(clazz, kv);

        return kv;
    }

    private void getFields(PsiClass clazz, KV kv) {
        for (PsiField field : clazz.getFields()) {
            kv.set(field.getName(), getField(field.getTypeElement()));
        }
    }

    private Object getField(PsiTypeElement element) {
        // 获取字段类型
        PsiType type = element.getType();
        String typeName = type.getPresentableText();

        // 如果是Object
        if ("Object".equals(typeName)) {
            return new Object();
        }
        // 基本类型
        else if (type instanceof PsiPrimitiveType) {
            return PsiTypesUtil.getDefaultValue(type);
        }
        // 数组
        else if (type instanceof PsiArrayType) {
            PsiTypeElement arrayElement = (PsiTypeElement) element.getFirstChild();
            return new Object[]{getField(arrayElement)};
        }
        // 列表
        else if (typeName.split("<")[0].contains("List") || typeName.split("<")[0].equals("Collection")) {
            PsiJavaCodeReferenceElement referenceElement = element.getInnermostComponentReferenceElement();
            PsiReferenceParameterList referenceParameter = referenceElement.getParameterList();
            PsiTypeElement[] generics = referenceParameter.getTypeParameterElements();

            // 不存在泛型
            if (generics.length == 0) return new Object[]{new Object()};

            // 获取泛型
            PsiTypeElement generic = generics[0];

            return new Object[]{getField(generic)};
        }
        // map
        else if (typeName.split("<")[0].contains("Map")) {
            PsiJavaCodeReferenceElement referenceElement = element.getInnermostComponentReferenceElement();
            PsiReferenceParameterList referenceParameter = referenceElement.getParameterList();
            PsiTypeElement[] generics = referenceParameter.getTypeParameterElements();

            // 不存在泛型
            if (generics.length == 0) return new Object();

            PsiTypeElement genericKey = generics[0];
            Object key = getField(genericKey);

            PsiTypeElement genericVal = generics[1];
            Object val = getField(genericVal);

            Map<Object, Object> map = new HashMap<>();
            map.put(key, val);
            return map;
        }
        // 其他类型
        else if (NormalTypeConst.isNormalType(typeName)) {
            return NormalTypeConst.get(typeName);
        }
        // 其他的类
        else {
            PsiClass clazz = PsiUtil.resolveClassInType(element.getType());
            return classParser(clazz);
        }
    }
}
