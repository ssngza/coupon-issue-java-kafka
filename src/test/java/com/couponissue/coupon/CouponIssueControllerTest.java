package com.couponissue.coupon;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CouponIssueControllerTest {
    private final CouponIssueService service = org.mockito.Mockito.mock(CouponIssueService.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new CouponIssueController(service, new AdminTokenValidator("secret-token")))
            .build();

    @Test
    void rejectsStockChangeWithoutValidAdminToken() throws Exception {
        mvc.perform(put("/api/coupons/1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\":10}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("유효한 관리자 인증 토큰이 필요합니다."));
    }

    @Test
    void changesStockWithValidAdminToken() throws Exception {
        when(service.stock(1L)).thenReturn(10L);

        mvc.perform(put("/api/coupons/1/stock")
                        .header("X-Admin-Token", "secret-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stock\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(10));

        verify(service).seedStock(eq(1L), eq(10L));
    }
}
