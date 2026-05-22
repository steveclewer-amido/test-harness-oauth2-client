package org.example.forgerock.web;

import org.example.forgerock.service.ApiCallService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class ApiCallsController {

    private final ApiCallService apiCallService;

    @Value("${app.introspection-uri:}")
    private String introspectionUri;

    public ApiCallsController(ApiCallService apiCallService) {
        this.apiCallService = apiCallService;
    }

    @GetMapping("/api-calls")
    public String apiCalls(Authentication auth,
                            HttpServletRequest request,
                            HttpServletResponse response,
                            Model model) {
        ApiCallService.ApiCallResult userInfo =
                apiCallService.callUserInfo(auth, request, response);
        model.addAttribute("userInfoResult", userInfo);

        boolean introspectionEnabled = StringUtils.hasText(introspectionUri);
        model.addAttribute("introspectionEnabled", introspectionEnabled);

        if (introspectionEnabled) {
            ApiCallService.ApiCallResult introspection =
                    apiCallService.callIntrospection(auth, request, response);
            model.addAttribute("introspectionResult", introspection);
        }

        return "api-calls";
    }
}
