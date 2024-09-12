package org.example.servlets;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.resourceProvider.ObservationResourceProvider;
import org.example.resourceProvider.PatientResourceProvider;

import java.io.IOException;
import java.sql.Connection;

public class FhirServlet extends RestfulServer {

    Connection connection = null;
    public FhirServlet(Connection connection){
        this.connection = connection;
    }
    @Override
    protected void initialize() throws ServletException {
        // Create a context for the appropriate version
        setFhirContext(FhirContext.forR4());

        // Register resource providers
        registerProvider(new PatientResourceProvider(this.connection ));

        registerProvider(new ObservationResourceProvider(this.connection));

        // Format the responses in nice HTML
        registerInterceptor(new ResponseHighlighterInterceptor());
    }

}
