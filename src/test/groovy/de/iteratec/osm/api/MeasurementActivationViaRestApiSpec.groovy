package de.iteratec.osm.api

import de.iteratec.osm.InMemoryConfigService
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification
/**
 * Created by nkuhn on 12.05.15.
 */
@TestFor(RestApiController)
@Mock([ApiKey])
class MeasurementActivationViaRestApiSpec extends Specification {

    private RestApiController controllerUnderTest
    InMemoryConfigService inMemoryConfigService

    static String apiKeyAllowed = 'allowed'
    static String apiKeyNotAllowed = 'not-allowed'

    def doWithSpring = {
        inMemoryConfigService(InMemoryConfigService)
    }

    void setup(){
        controllerUnderTest = controller
        //test data common to all tests
        ApiKey.withTransaction {
            new ApiKey(secretKey: apiKeyAllowed, valid: true, allowedForMeasurementActivation: true).save(failOnError: true)
            new ApiKey(secretKey: apiKeyNotAllowed, valid: true, allowedForMeasurementActivation: false).save(failOnError: true)
        }
        //mocks common to all tests
//        mockFilters(SecureApiFunctionsFilters)
        inMemoryConfigService = grailsApplication.mainContext.getBean('inMemoryConfigService')
        controllerUnderTest.inMemoryConfigService = inMemoryConfigService
    }

    // successful calls /////////////////////////////////////////////////////////////////////

    void "successful activation of measurements"(){
        when:
        params.apiKey = apiKeyAllowed
        MeasurementActivationCommand cmd = new MeasurementActivationCommand(
                apiKey: apiKeyAllowed,
                activationToSet: true
        )
        cmd.validate()
        controllerUnderTest.securedViaApiKeySetMeasurementActivation(cmd)

        then:
        response.status == 200
        response.text == "Set measurements activation to: true"
        inMemoryConfigService.areMeasurementsGenerallyEnabled() == true
    }

    void "successful deactivation of measurements"(){
        when:
        params.apiKey = apiKeyAllowed
        MeasurementActivationCommand cmd = new MeasurementActivationCommand(
                apiKey: apiKeyAllowed,
                activationToSet: false
        )
        cmd.validate()
        controllerUnderTest.securedViaApiKeySetMeasurementActivation(cmd)

        then:
        response.status == 200
        response.text == "Set measurements activation to: false"
        inMemoryConfigService.areMeasurementsGenerallyEnabled() == false
    }

    // failing calls /////////////////////////////////////////////////////////////////////

    void "should fail cause of api key without permission"(){
        setup:
        boolean defaultPermission = false

        when:
        params.apiKey = apiKeyNotAllowed
        MeasurementActivationCommand cmd = new MeasurementActivationCommand(
                apiKey: apiKeyNotAllowed,
                activationToSet: true
        )
        cmd.validate()
        controllerUnderTest.securedViaApiKeySetMeasurementActivation(cmd)

        then:
        inMemoryConfigService.areMeasurementsGenerallyEnabled() == defaultPermission
        response.status == 400
        response.contentAsString == "Error field apiKey: "+RestApiController.DEFAULT_ACCESS_DENIED_MESSAGE+"\n"
    }

    void "should fail cause of missing boolean activationToSet"(){
        setup:
        boolean defaultPermission = false

        when:
        params.apiKey = apiKeyAllowed
        MeasurementActivationCommand cmd = new MeasurementActivationCommand(
                apiKey: apiKeyAllowed
        )
        cmd.validate()
        controllerUnderTest.securedViaApiKeySetMeasurementActivation(cmd)

        then:
        inMemoryConfigService.areMeasurementsGenerallyEnabled() == defaultPermission
        response.status == 400
        response.text == "Error field activationToSet: nullable\n"
    }
}
