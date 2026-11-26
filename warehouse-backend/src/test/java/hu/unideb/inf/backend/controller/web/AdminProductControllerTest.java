package hu.unideb.inf.backend.controller.web;

import hu.unideb.inf.backend.config.ClockConfig;
import hu.unideb.inf.backend.service.facade.AdminViewFacade;
import hu.unideb.inf.core.dto.ProductRequest;
import hu.unideb.inf.core.dto.ProductResponse;
import hu.unideb.inf.core.enums.ZoneType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminProductController.class)
@Import(ClockConfig.class)
class AdminProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminViewFacade adminViewFacade;

    private ProductResponse testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new ProductResponse(
                1L,
                "SKU-1092",
                "5901234567890",
                "Fékfolyadék DOT4 500ml",
                "Nagy teljesítményű szintetikus fékfolyadék",
                BigDecimal.valueOf(0.600).setScale(3),
                BigDecimal.valueOf(0.000800).setScale(6),
                true,
                ZoneType.HAZARDOUS,
                10,
                true,
                Instant.now(),
                Instant.now()
        );

        given(adminViewFacade.getCurrentUsername()).willReturn("Adminisztrátor");
        given(adminViewFacade.getCurrentUserRole()).willReturn("Diszpécser");
        given(adminViewFacade.getProducts(any(), any())).willReturn(List.of(testProduct));
        given(adminViewFacade.getProductById(1L)).willReturn(testProduct);
    }

    @Test
    @DisplayName("GET /admin/products should return 200 OK, catalog view and product list")
    void listProducts_shouldReturn200AndListView() throws Exception {
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products/list"))
                .andExpect(model().attribute("activeMenu", "products"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("zoneTypes"))
                .andExpect(content().string(containsString("Terméktörzs Katalógus")))
                .andExpect(content().string(containsString("SKU-1092")))
                .andExpect(content().string(containsString("Fékfolyadék DOT4")))
                .andExpect(content().string(containsString("Veszélyes")));
    }

    @Test
    @DisplayName("GET /admin/products with search query and zoneType should pass filter parameters")
    void listProducts_withFilters_shouldPassParametersToFacade() throws Exception {
        mockMvc.perform(get("/admin/products")
                        .param("query", "DOT4")
                        .param("zoneType", "HAZARDOUS"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products/list"))
                .andExpect(model().attribute("query", "DOT4"))
                .andExpect(model().attribute("selectedZoneType", ZoneType.HAZARDOUS));

        verify(adminViewFacade).getProducts("DOT4", ZoneType.HAZARDOUS);
    }

    @Test
    @DisplayName("GET /admin/products/new should return 200 OK and empty product form")
    void showCreateForm_shouldReturn200AndFormView() throws Exception {
        mockMvc.perform(get("/admin/products/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products/form"))
                .andExpect(model().attribute("isEdit", false))
                .andExpect(model().attributeExists("productRequest"))
                .andExpect(content().string(containsString("Új Termék Rögzítése")));
    }

    @Test
    @DisplayName("POST /admin/products with valid data should save product and redirect to /admin/products (302)")
    void createProduct_withValidData_shouldRedirect() throws Exception {
        mockMvc.perform(post("/admin/products")
                        .param("sku", "SKU-9999")
                        .param("barcode", "5901234567899")
                        .param("name", "Teszt Termék")
                        .param("description", "Teszt leírás")
                        .param("unitWeightKg", "1.500")
                        .param("unitVolumeM3", "0.002000")
                        .param("requiredZoneType", "NORMAL")
                        .param("minStockLevel", "5")
                        .param("requiresExpiry", "true")
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(adminViewFacade).createProduct(any(ProductRequest.class));
    }

    @Test
    @DisplayName("POST /admin/products with invalid data should return 200 and show form with errors")
    void createProduct_withInvalidData_shouldReturnFormWithErrors() throws Exception {
        mockMvc.perform(post("/admin/products")
                        .param("sku", "")
                        .param("barcode", "")
                        .param("name", "")
                        .param("unitWeightKg", "-1.0")
                        .param("unitVolumeM3", "-0.5"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products/form"))
                .andExpect(model().hasErrors());

        verify(adminViewFacade, never()).createProduct(any());
    }

    @Test
    @DisplayName("GET /admin/products/{id}/edit should return 200 OK and prefilled form")
    void showEditForm_shouldReturn200AndPrefilledForm() throws Exception {
        mockMvc.perform(get("/admin/products/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products/form"))
                .andExpect(model().attribute("isEdit", true))
                .andExpect(model().attribute("productId", 1L))
                .andExpect(content().string(containsString("Termék Szerkesztése")))
                .andExpect(content().string(containsString("SKU-1092")));
    }

    @Test
    @DisplayName("POST /admin/products/{id} with valid data should update and redirect to /admin/products (302)")
    void updateProduct_withValidData_shouldRedirect() throws Exception {
        mockMvc.perform(post("/admin/products/1")
                        .param("sku", "SKU-1092")
                        .param("barcode", "5901234567890")
                        .param("name", "Fékfolyadék Módosított")
                        .param("description", "Frissített leírás")
                        .param("unitWeightKg", "0.650")
                        .param("unitVolumeM3", "0.000900")
                        .param("requiresExpiry", "false")
                        .param("requiredZoneType", "HAZARDOUS")
                        .param("minStockLevel", "12")
                        .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(adminViewFacade).updateProduct(eq(1L), any(ProductRequest.class));
    }

    @Test
    @DisplayName("POST /admin/products/{id} with invalid data should return 200 and show errors")
    void updateProduct_withInvalidData_shouldReturnFormWithErrors() throws Exception {
        mockMvc.perform(post("/admin/products/1")
                        .param("sku", "")
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products/form"))
                .andExpect(model().hasErrors());

        verify(adminViewFacade, never()).updateProduct(any(), any());
    }
}
