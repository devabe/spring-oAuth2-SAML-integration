package com.ohadr.saml;

import com.sun.javafx.binding.StringFormatter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;

@Controller
@PreAuthorize("hasAuthority('ROLE_USER')")
public class IndexController {

    @RequestMapping(value = "/oauth/authorizedResource", method = RequestMethod.GET)
    public @ResponseBody String resource(Principal principal) {
        return "{'resource-from':'Resource Server','granted-to':'" +principal.getName()+ "','message':'I am a Resource server and you are Granted successfully !'}";
    }

}
