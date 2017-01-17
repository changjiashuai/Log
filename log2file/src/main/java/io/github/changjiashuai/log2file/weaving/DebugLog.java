package io.github.changjiashuai.log2file.weaving;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

/**
 * Email: changjiashuai@gmail.com
 *
 * Created by CJS on 2017/1/17 17:12.
 */
@Target({TYPE, METHOD, CONSTRUCTOR}) @Retention(RetentionPolicy.CLASS)
public @interface DebugLog {
}
