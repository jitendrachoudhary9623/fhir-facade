package org.example.resourceProvider;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ObservationResourceProvider implements IResourceProvider {

    private Connection connection;

    public ObservationResourceProvider(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Class<? extends IBaseResource> getResourceType() {
        return Observation.class;
    }

    // Read an observation by ID (both for blood pressure and heart rate)
    @Read()
    public Observation read(@IdParam IdType theId) {
        Observation observation = null;

        // Try reading from blood pressure table
        observation = getBloodPressureById(Integer.parseInt(theId.getIdPart()));
        if (observation == null) {
            // If not found, try heart rate table
            observation = getHeartRateById(Integer.parseInt(theId.getIdPart()));
        }

        if (observation == null) {
            throw new ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException(theId);
        }

        return observation;
    }

    // Search for observations by patient ID (returns both blood pressure and heart rate observations)
    @Search
    public List<Observation> searchByPatientId(@OptionalParam(name = Observation.SP_SUBJECT) String patientId) {
        List<Observation> observations = new ArrayList<>();

        // Get blood pressure observations for the patient
        observations.addAll(getBloodPressuresByPatientId(Integer.parseInt(patientId)));

        // Get heart rate observations for the patient
        observations.addAll(getHeartRatesByPatientId(Integer.parseInt(patientId)));

        return observations;
    }

    // Helper method to retrieve a blood pressure observation by ID
    private Observation getBloodPressureById(int id) {
        String sql = "SELECT id, patient_id, systolic, diastolic, date FROM blood_pressure WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapBloodPressureToObservation(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Helper method to retrieve heart rate observation by ID
    private Observation getHeartRateById(int id) {
        String sql = "SELECT id, patient_id, rate, date FROM heart_rate WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapHeartRateToObservation(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Helper method to retrieve blood pressure observations by patient ID
    private List<Observation> getBloodPressuresByPatientId(int patientId) {
        List<Observation> observations = new ArrayList<>();
        String sql = "SELECT id, patient_id, systolic, diastolic, date FROM blood_pressure WHERE patient_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                observations.add(mapBloodPressureToObservation(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return observations;
    }

    // Helper method to retrieve heart rate observations by patient ID
    private List<Observation> getHeartRatesByPatientId(int patientId) {
        List<Observation> observations = new ArrayList<>();
        String sql = "SELECT id, patient_id, rate, date FROM heart_rate WHERE patient_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                observations.add(mapHeartRateToObservation(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return observations;
    }

    @Search
    public List<Observation> getAll() {
        List<Observation> observations = new ArrayList<>();

        // Get all blood pressure observations
        observations.addAll(getAllBloodPressures());

        // Get all heart rate observations
        observations.addAll(getAllHeartRates());

        return observations;
    }

    // Helper method to retrieve all blood pressure observations
    private List<Observation> getAllBloodPressures() {
        List<Observation> observations = new ArrayList<>();
        String sql = "SELECT id, patient_id, systolic, diastolic, date FROM blood_pressure";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                observations.add(mapBloodPressureToObservation(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return observations;
    }

    // Helper method to retrieve all heart rate observations
    private List<Observation> getAllHeartRates() {
        List<Observation> observations = new ArrayList<>();
        String sql = "SELECT id, patient_id, rate, date FROM heart_rate";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                observations.add(mapHeartRateToObservation(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return observations;
    }

    // Map a ResultSet row from blood pressure table to an FHIR Observation
    private Observation mapBloodPressureToObservation(ResultSet rs) throws SQLException {
        Observation observation = new Observation();

        // Set observation ID
        observation.setId(String.valueOf(rs.getInt("id")));

        // Set observation code for blood pressure
        observation.getCode().addCoding()
                .setSystem("http://loinc.org")
                .setCode("85354-9")  // LOINC code for Blood Pressure
                .setDisplay("Blood pressure systolic & diastolic");

        // Set patient reference
        observation.getSubject().setReference("Patient/" + rs.getInt("patient_id"));

        // Add systolic value
        observation.addComponent()
                .getCode().addCoding()
                .setSystem("http://loinc.org")
                .setCode("8480-6") // LOINC code for Systolic Blood Pressure
                .setDisplay("Systolic Blood Pressure");

        observation.getComponent().get(observation.getComponent().size() - 1)
                .setValue(new org.hl7.fhir.r4.model.Quantity()
                        .setValue(rs.getInt("systolic"))
                        .setUnit("mmHg"));

        // Add diastolic value
        observation.addComponent()
                .getCode().addCoding()
                .setSystem("http://loinc.org")
                .setCode("8462-4") // LOINC code for Diastolic Blood Pressure
                .setDisplay("Diastolic Blood Pressure");

        observation.getComponent().get(observation.getComponent().size() - 1)
                .setValue(new org.hl7.fhir.r4.model.Quantity()
                        .setValue(rs.getInt("diastolic"))
                        .setUnit("mmHg"));

        // Set observation date
        observation.setEffective(new org.hl7.fhir.r4.model.DateTimeType(rs.getDate("date")));

        return observation;

    }

    // Map a ResultSet row from heart rate table to an FHIR Observation
    private Observation mapHeartRateToObservation(ResultSet rs) throws SQLException {
        Observation observation = new Observation();

        // Set observation ID
        observation.setId(String.valueOf(rs.getInt("id")));

        // Set observation code for heart rate
        observation.getCode().addCoding()
                .setSystem("http://loinc.org")
                .setCode("8867-4")  // LOINC code for Heart Rate
                .setDisplay("Heart rate");

        // Set patient reference
        observation.getSubject().setReference("Patient/" + rs.getInt("patient_id"));

        // Set heart rate value
        observation.setValue(new org.hl7.fhir.r4.model.Quantity(rs.getInt("rate")).setUnit("bpm"));

        // Set observation date
        observation.setEffective(new org.hl7.fhir.r4.model.DateTimeType(rs.getDate("date")));

        return observation;
    }
}
