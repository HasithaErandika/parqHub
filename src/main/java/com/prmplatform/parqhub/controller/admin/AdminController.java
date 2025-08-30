package com.prmplatform.parqhub.controller.admin;

import com.prmplatform.parqhub.repository.AdminRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminRepository adminRepository;

    public AdminController(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "admin-login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        Model model) {
        return adminRepository.findByEmailAndPassword(email, password)
                .map(admin -> "redirect:/admin/dashboard") // success
                .orElseGet(() -> {
                    model.addAttribute("error", "Invalid email or password");
                    return "admin-login"; // back to login page with error
                });
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "admin/dashboard"; // Looks for admin-dashboard.html
    }
}
