package com.amahoro.amahoro_stadium_ticketing.controller;

import com.amahoro.amahoro_stadium_ticketing.model.AmahoroEntity;
import com.amahoro.amahoro_stadium_ticketing.repository.AmahoroRepository;
import com.amahoro.amahoro_stadium_ticketing.service.AmahoroService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
public class AmahoroController {

//    @Autowired
//    private JavaMailSender emailSender;
    @Autowired
    private AmahoroRepository amahoroRepository;

    @Autowired
    AmahoroService amahoroService;

    @RequestMapping(value = {"/view"}, method = RequestMethod.GET)
    public String showList(Model model) {
        System.out.println("I am inside the view controller");
        List<AmahoroEntity> viewclient = amahoroService.listAll();
        model.addAttribute("viewclient", viewclient);
        return "admin/viewclient";
    }

    @GetMapping("/query")
    public String showQueries(Model model) {
        List<AmahoroEntity> viewclient = amahoroRepository.findAll();
        model.addAttribute("viewclient", viewclient);
        return "admin/clientQueriesReply";
    }

//    @PostMapping("/sendEmail")
//    public String sendEmail(@RequestParam String email, @RequestParam String message) {
//        SimpleMailMessage mailMessage = new SimpleMailMessage();
//        mailMessage.setTo(email);
//        mailMessage.setSubject("Meeting Request");
//        mailMessage.setText(message);
//        emailSender.send(mailMessage);
//        return "Admin/dashboard";
//    }

    @GetMapping("/edit/{id}")
    public String editArena(@PathVariable("id") Long id, Model model) {
        System.out.println("im inside edit controller");
        AmahoroEntity viewClient= amahoroRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid booking Id:" + id));
        model.addAttribute("viewClient", viewClient);
        return "admin/update";
    }

    @PostMapping("/update/{id}")
    public String update(@PathVariable(value = "id") long id, @Valid AmahoroEntity viewClient, BindingResult bindingResult) throws IOException {
        if (bindingResult.hasErrors()) {
            viewClient.setId(id);
            return "admin/dashboard";
        }
        amahoroRepository.save(viewClient);
        return "redirect:/view";
    }


    @RequestMapping(value = "/image/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> getImage(@PathVariable("id") Long id) {
        Optional<AmahoroEntity> booking = amahoroService.findClientById(id);
        byte[] imageBytes = booking.get().getProfilePhoto();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.setContentLength(imageBytes.length);
        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }


    @RequestMapping(value = {"/client"}, method = RequestMethod.GET)
    public String showForm(Model model) {
        model.addAttribute("booking", new AmahoroEntity());
        System.out.println("I am inside client form");
        return "booking";
    }



    @PostMapping("/clientform")
    public String submitForm(@ModelAttribute("booking") @Valid AmahoroEntity booking,
                             BindingResult bindingResult,
                             @RequestParam(value = "file", required = false) MultipartFile profilePhoto) throws IOException {
        if (bindingResult.hasErrors()) {
            System.out.println("Binding result has errors");
        }

        if (!profilePhoto.isEmpty()) {
            String contentType = profilePhoto.getContentType();
            if (contentType.equals("image/jpeg") || contentType.equals("image/png")) {
                byte[] imageBytes = profilePhoto.getBytes();
                booking.setProfilePhoto(imageBytes);
            } else {
                bindingResult.rejectValue("profilePhoto", "error.profilePhoto", "Invalid file type");
                return "admin/dashboard";
            }
        }

        amahoroRepository.save(booking);


            return "user/dashboard";
    }

    @GetMapping("/searchBooking")
    public String searchBooking(@RequestParam(value = "clientName", required = false) String clientName, Model model) {
        List<AmahoroEntity> results;
        if (clientName != null && !clientName.isEmpty()) {
            results = amahoroService.findByClientName(clientName);
        } else {
            results = amahoroService.listAll(); // Show all records if search is empty
        }
        model.addAttribute("viewclient", results);
        return "admin/viewclient"; // Template to display search results
    }

    @GetMapping("/viewSorted")
    public String showSortedList(Model model) {
        List<AmahoroEntity> sortedClients = amahoroService.listAllSortedByName();
        model.addAttribute("viewclient", sortedClients);
        return "admin/viewclient";
    }
    // In AmahoroController or UserController, update the URL
    @GetMapping("/admin-dashboard")
    public String showDashboard() {
        return "admin/dashboard";  // Dashboard template path
    }


    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadBookingPdf(@PathVariable Long id) {
        try {
            Optional<AmahoroEntity> booking = amahoroService.getBookingById(id);
            if (booking.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            // Use FontFactory to get Helvetica
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Booking Details", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Add booking information
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            document.add(new Paragraph("Booking ID: " + booking.get().getId(), textFont));
            document.add(new Paragraph("Client Name: " + booking.get().getClientName(), textFont));
            document.add(new Paragraph("Client Email: " + booking.get().getEmail(), textFont));
            document.add(new Paragraph("Phone Number: " + booking.get().getPhoneNumber(), textFont));
            document.add(new Paragraph("Ticket Class: " + booking.get().getTicketclass(), textFont));
            document.add(new Paragraph(" "));  // Empty line for spacing

            // Add profile picture
            byte[] imageBytes = booking.get().getProfilePhoto();
            if (imageBytes != null && imageBytes.length > 0) {
                try {
                    Image profileImage = Image.getInstance(imageBytes);
                    profileImage.setAlignment(Element.ALIGN_CENTER);
                    profileImage.scaleToFit(150, 150); // Adjust size as needed
                    document.add(profileImage);
                } catch (IOException | BadElementException e) {
                    e.printStackTrace();
                }
            }

            document.close();
            byte[] pdfBytes = baos.toByteArray();
            ByteArrayResource resource = new ByteArrayResource(pdfBytes);

            // Set download filename with booking ID
            String filename = "Booking_Details_" + booking.get().getId() + ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .body(resource);
        } catch (DocumentException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/delete/{id}")
    public String deleteClient(@PathVariable("id") Long id) {
        AmahoroEntity client = amahoroService.findClientById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid client Id:" + id));

        amahoroRepository.delete(client);

        return "redirect:/view";
    }


}
