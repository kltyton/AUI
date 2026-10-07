package io.github.kltyton.kltytonui.registry;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.init.Document;
import io.github.kltyton.kltytonui.init.Element;
import io.github.kltyton.kltytonui.registry.annotation.ElementRegister;
import io.github.kltyton.kltytonui.spi.KuiServices;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class KltytonUIRegistry {
    public static List<Element> ELEMENTS = new ArrayList<>();
    public static void scanPackage(String basePackage) {
        KuiServices.client().addScanPackage(basePackage);
    }

    public static void scanPackages(String... basePackages) {
        KuiServices.client().addScanPackages(basePackages);
    }

    public static void register() {
        KuiServices.client().scanAnnotationClasses(ElementRegister.class, data -> true, clazz -> {
            if (!Element.class.isAssignableFrom(clazz)) {
                KltytonUI.LOGGER.error("Class {} has @ElementRegister but is not a subclass of Element!", clazz.getName());
                return;
            }

            ElementRegister annotation = clazz.getAnnotation(ElementRegister.class);
            String value = annotation.value();
            Element.register(value, (document, s) -> {
                try {
                    Constructor<?> constructor = clazz.getConstructor(Document.class);
                    constructor.setAccessible(true);
                    Element element = (Element) constructor.newInstance(document);
                    ELEMENTS.add(element);
                    return element;
                } catch (Throwable throwable) {
                    KltytonUI.LOGGER.error("Failed to load element {}", clazz.getName(), throwable);
                    return new Element(document, value);
                }
            });
        }, () -> {
        });
    }
}
