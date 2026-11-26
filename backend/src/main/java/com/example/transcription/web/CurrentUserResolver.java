package com.example.transcription.web;

import com.example.transcription.repo.AppUserRepository;
import com.example.transcription.service.CurrentUser;
import com.example.transcription.service.Errors;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** POC identity: the role switcher sends X-User-Id. Replace with real auth (e.g. Entra ID) later. */
@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {
    private final AppUserRepository users;

    public CurrentUserResolver(AppUserRepository users) { this.users = users; }

    @Override
    public boolean supportsParameter(MethodParameter p) { return p.getParameterType() == CurrentUser.class; }

    @Override
    public Object resolveArgument(MethodParameter p, ModelAndViewContainer m, NativeWebRequest req, WebDataBinderFactory f) {
        String header = req.getHeader("X-User-Id");
        if (header == null) throw new Errors.Unauthorized("Missing X-User-Id header");
        try {
            return users.findById(Long.parseLong(header.trim()))
                    .map(u -> new CurrentUser(u.id, u.name, u.role))
                    .orElseThrow(() -> new Errors.Unauthorized("Unknown user"));
        } catch (NumberFormatException e) {
            throw new Errors.Unauthorized("Invalid X-User-Id");
        }
    }
}
