package io.github.hyeonsulee.wsdocs.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excludes a handler method, a publisher method or a whole class from the generated document.
 *
 * <p>On a class, every handler and publication declared in it is skipped. On a method, only that method is
 * skipped; its class stays documented.
 *
 * <pre>{@code
 * @Controller
 * public class AdminController {
 *
 *     @WsHidden
 *     @MessageMapping("/admin/reset")
 *     public void reset() { ... }
 * }
 * }</pre>
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface WsHidden {
}
