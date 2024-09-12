package org.example.resourceProvider;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatientResourceProvider implements IResourceProvider {

    private Connection connection = null;
    public PatientResourceProvider(Connection connection){
        this.connection = connection;
    }

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Patient.class;
    }

    @Read()
    public Patient read(@IdParam IdType theId) {
        Patient patient = null;
        String sql = "SELECT id, first_name, last_name, date_of_birth FROM patients WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, Integer.parseInt(theId.getIdPart()));
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                patient = mapResultSetToPatient(rs);
            } else {
                throw new ResourceNotFoundException(theId);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new ResourceNotFoundException(theId); // Handle exceptions properly
        }

        return patient;
    }

    // Search for all patients from PostgreSQL
    @Search
    public List<Patient> getAll() {
        List<Patient> patients = new ArrayList<>();
        String sql = "SELECT id, first_name, last_name, date_of_birth FROM patients";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Patient patient = mapResultSetToPatient(rs);
                patients.add(patient);
            }

        } catch (SQLException e) {
            e.printStackTrace(); // Handle exceptions properly
        }

        return patients;
    }

    // Helper method to convert a ResultSet row into a FHIR Patient resource
    private Patient mapResultSetToPatient(ResultSet rs) throws SQLException {
        Patient patient = new Patient();

        // Set patient ID
        patient.setId(rs.getString("id"));

        // Set patient name
        patient.addName().setFamily(rs.getString("last_name")).addGiven(rs.getString("first_name"));

        // Set patient date of birth
        if (rs.getDate("date_of_birth") != null) {
            patient.setBirthDate(rs.getDate("date_of_birth"));
        }

        return patient;
    }
}
