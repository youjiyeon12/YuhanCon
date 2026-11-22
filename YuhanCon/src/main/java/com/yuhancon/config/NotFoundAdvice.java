package com.yuhancon.config;

import java.io.IOException;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletResponse;

// /concertDetail/abc처럼 주소의 id 자리가 숫자가 아니면 "없는 페이지"(404)로 안내한다.
// 폼 값(예: 좌석 수)이 잘못된 경우는 그대로 400이다. sendError로 넘기면 error.html이 상태별 안내를 보여 준다.
@ControllerAdvice
public class NotFoundAdvice {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public void typeMismatch(MethodArgumentTypeMismatchException ex, HttpServletResponse response) throws IOException {
        boolean pathId = ex.getParameter().hasParameterAnnotation(PathVariable.class);
        response.sendError(pathId ? HttpServletResponse.SC_NOT_FOUND : HttpServletResponse.SC_BAD_REQUEST);
    }
}
