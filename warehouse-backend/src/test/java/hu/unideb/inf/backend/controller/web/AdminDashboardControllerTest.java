package hu.unideb.inf.backend.controller.web;

import hu.unideb.inf.backend.config.ClockConfig;
import hu.unideb.inf.backend.service.facade.AdminViewFacade;
import hu.unideb.inf.backend.service.facade.dto.StockAlertDto;
import hu.unideb.inf.backend.service.facade.dto.ZoneLoadDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminDashboardController.class)
@Import(ClockConfig.class)
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminViewFacade adminViewFacade;

    @BeforeEach
    void setUp() {
        given(adminViewFacade.getTotalProducts()).willReturn(142);
        given(adminViewFacade.getOccupancyRate()).willReturn("78%");
        given(adminViewFacade.getCriticalStockCount()).willReturn(5);
        given(adminViewFacade.getActiveTasksCount()).willReturn(12);
        given(adminViewFacade.getCurrentUsername()).willReturn("Adminisztrátor");
        given(adminViewFacade.getCurrentUserRole()).willReturn("Diszpécser");

        List<ZoneLoadDto> zoneLoads = List.of(
                new ZoneLoadDto("A-Zóna (Száraz raktár - Normál)", 4500, 6000, 75, false, "bg-primary", "text-muted"),
                new ZoneLoadDto("B-Zóna (Hűtött raktár)", 1920, 2000, 96, true, "bg-danger", "text-danger fw-bold")
        );
        given(adminViewFacade.getZoneLoads()).willReturn(zoneLoads);

        List<StockAlertDto> stockAlerts = List.of(
                new StockAlertDto("Fékfolyadék DOT4 (SKU-1092)", "Készlet: 3 db (Min: 10 db)", "danger", null, "Rendelés"),
                new StockAlertDto("Szintetikus Motorolaj 5W30 (Lot-44)", "Lejár: 14 napon belül", "warning", "Polc: A-02-1", null)
        );
        given(adminViewFacade.getStockAlerts()).willReturn(stockAlerts);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin", "/admin/", "/admin/dashboard"})
    @DisplayName("Should return 200 OK and dashboard view with model attributes for all dashboard URL variants")
    void testDashboardEndpointsReturn200AndModel(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attribute("activeMenu", "dashboard"))
                .andExpect(model().attribute("totalProducts", 142))
                .andExpect(model().attribute("occupancyRate", "78%"))
                .andExpect(model().attribute("criticalStockCount", 5))
                .andExpect(model().attribute("activeTasks", 12))
                .andExpect(model().attributeExists("zoneLoads"))
                .andExpect(model().attributeExists("stockAlerts"))
                .andExpect(model().attribute("currentUsername", "Adminisztrátor"))
                .andExpect(model().attribute("currentUserRole", "Diszpécser"))
                .andExpect(content().string(containsString("Raktár Felügyeleti Dashboard")))
                .andExpect(content().string(containsString("Vezérlőpult")))
                .andExpect(content().string(containsString("A-Zóna")))
                .andExpect(content().string(containsString("Fékfolyadék DOT4")));
    }
}
