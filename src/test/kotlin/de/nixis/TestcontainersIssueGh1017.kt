package de.bringmeister.connect.ordermagentofacade

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootApplication
class Application

@Component
class SomeSimpleBean

@Testcontainers // Has to run before SpringBootTest (SpringExtension) is run,
@SpringBootTest // but because of <constructorInjectedBean> below this doesn't happen.
@ContextConfiguration(initializers = [TestcontainersIssueGh1017ConstructorInjection.DynamoDbAwareInitializer::class])
class TestcontainersIssueGh1017ConstructorInjection(
    @Autowired private val constructorInjectedBean: SomeSimpleBean
) {

    companion object {
        @Container
        private val dynamodb = KGenericContainer("richnorth/dynalite:latest")
            .withExposedPorts(4567)
    }

    @Test
    fun `should execute TestcontainerExtension before SpringExtention`() {
        // When we reach here we're fine
    }

    internal class DynamoDbAwareInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(context: ConfigurableApplicationContext) {
            // FIXME Next line will fail with
            // > java.lang.IllegalStateException: Mapped port can only be obtained after the container is started
            val dynamodbEndpoint = "http://${dynamodb.containerIpAddress}:${dynamodb.getMappedPort(4567)}"
            TestPropertyValues.of(
                "aws.dynamodb.endpoint: $dynamodbEndpoint"
            ).applyTo(context)
        }
    }
}

@Testcontainers // Runs before SpringBootTest as expected
@SpringBootTest // when using field injection.
@ContextConfiguration(initializers = [TestcontainersIssueGh1017FieldInjection.DynamoDbAwareInitializer::class])
class TestcontainersIssueGh1017FieldInjection {

    companion object {
        @Container
        private val dynamodb = KGenericContainer("richnorth/dynalite:latest")
            .withExposedPorts(4567)
    }

    @Autowired
    private lateinit var fieldInjectedBean: SomeSimpleBean

    @Test
    fun `should execute TestcontainerExtension before SpringExtention`() {
        // When we reach here we're fine
    }

    internal class DynamoDbAwareInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(context: ConfigurableApplicationContext) {
            val dynamodbEndpoint = "http://${dynamodb.containerIpAddress}:${dynamodb.getMappedPort(4567)}"
            Thread.sleep(1000)
            TestPropertyValues.of(
                "aws.dynamodb.endpoint: $dynamodbEndpoint"
            ).applyTo(context)
        }
    }
}

@Testcontainers // Runs before SpringBootTest as expected
@SpringBootTest // when no bean injection happens.
@ContextConfiguration(initializers = [TestcontainersIssueGh1017WithoutInjection.DynamoDbAwareInitializer::class])
class TestcontainersIssueGh1017WithoutInjection {

    companion object {
        @Container
        private val dynamodb = KGenericContainer("richnorth/dynalite:latest")
            .withExposedPorts(4567)
    }

//    @Autowired
//    private lateinit var noInjectedBean: SomeSimpleBean

    @Test
    fun `should execute TestcontainerExtension before SpringExtention`() {
        // When we reach here we're fine
    }

    internal class DynamoDbAwareInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
        override fun initialize(context: ConfigurableApplicationContext) {
            val dynamodbEndpoint = "http://${dynamodb.containerIpAddress}:${dynamodb.getMappedPort(4567)}"
            TestPropertyValues.of(
                "aws.dynamodb.endpoint: $dynamodbEndpoint"
            ).applyTo(context)
        }
    }
}

@SpringBootTest // Because of inversed annotation order this runs before
@Testcontainers // testcontainers is run -> as expected.
@ContextConfiguration(initializers = [TestcontainersIssueGh1017WithoutInjectionInversedAnnotationOrder.DynamoDbAwareInitializer::class])
class TestcontainersIssueGh1017WithoutInjectionInversedAnnotationOrder {

    companion object {

        @Container
        private val dynamodb = KGenericContainer("richnorth/dynalite:latest")
            .withExposedPorts(4567)
    }

//    @Autowired
//    private lateinit var noInjectedBean: SomeSimpleBean

    @Test
    fun `should execute TestcontainerExtension before SpringExtention`() {
        // It is actually expected that we don't reach her due to annotation order.
    }

    internal class DynamoDbAwareInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

        override fun initialize(context: ConfigurableApplicationContext) {
            // Next line fails, but that is actually expected.
            val dynamodbEndpoint = "http://${dynamodb.containerIpAddress}:${dynamodb.getMappedPort(4567)}"
            TestPropertyValues.of(
                "aws.dynamodb.endpoint: $dynamodbEndpoint"
            ).applyTo(context)
        }
    }
}

class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)
