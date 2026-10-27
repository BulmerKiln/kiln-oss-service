/*
 * Copyright The Bulmer Kiln Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.bulmerkiln.oss;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Kiln for one organization: the components, plus the few parts that are this edition's own. */
@SpringBootApplication(proxyBeanMethods = false)
public final class KilnOssApplication {

    private KilnOssApplication() {}

    public static void main(String[] args) {
        SpringApplication.run(KilnOssApplication.class, args);
    }
}
