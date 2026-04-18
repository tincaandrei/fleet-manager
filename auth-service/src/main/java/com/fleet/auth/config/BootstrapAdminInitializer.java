package com.fleet.auth.config;

import com.fleet.auth.service.UserInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final BootstrapAdminProperties bootstrapAdminProperties;
    private final UserInfoService userInfoService;

    @Override
    public void run(ApplicationArguments args) {
        userInfoService.ensureBootstrapAdmin(bootstrapAdminProperties);
    }
}
