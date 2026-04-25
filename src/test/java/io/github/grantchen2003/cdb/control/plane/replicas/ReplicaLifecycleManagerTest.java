package io.github.grantchen2003.cdb.control.plane.replicas;

import io.github.grantchen2003.cdb.control.plane.config.replica.ReplicaConfig;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.ApplierProvisionerFactory;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.Ec2InstanceProvisioner;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.StorageEngineProvisionerFactory;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.TxManagerProvisionerFactory;
import io.github.grantchen2003.cdb.control.plane.writeschemas.WriteSchema;
import io.github.grantchen2003.cdb.control.plane.writeschemas.WriteSchemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplicaLifecycleManagerTest {

    private static final String USER_ID        = "user-123";
    private static final String CHRONICLE_ID   = "chronicle-123";
    private static final String CHRONICLE_NAME = "my-chronicle";
    private static final String APPLIER_ID     = "i-applier-123";
    private static final String STORAGE_ID     = "i-storage-123";
    private static final String TX_MANAGER_ID  = "i-txmanager-123";
    private static final String PUBLIC_IP      = "203.0.113.10";

    private static final Replica PROVISIONING_REPLICA = new Replica(
            "replica-123", USER_ID, CHRONICLE_ID, CHRONICLE_NAME, ReplicaType.REDIS,
            APPLIER_ID, STORAGE_ID, TX_MANAGER_ID,
            null, ReplicaStatus.PROVISIONING, Instant.now().minusSeconds(30)
    );

    private static final Replica NEW_REPLICA = new Replica(
            "replica-123", USER_ID, CHRONICLE_ID, CHRONICLE_NAME, ReplicaType.REDIS,
            null, null, null,
            null, ReplicaStatus.NEW, Instant.now()
    );

    @Mock private ReplicaConfig replicaConfig;
    @Mock private ReplicaRepository replicaRepository;
    @Mock private WriteSchemaService writeSchemaService;
    @Mock private Ec2Client ec2Client;
    @Mock private ApplierProvisionerFactory applierProvisionerFactory;
    @Mock private StorageEngineProvisionerFactory storageEngineProvisionerFactory;
    @Mock private TxManagerProvisionerFactory txManagerProvisionerFactory;
    @Mock private Ec2InstanceProvisioner storageEngineProvisioner;
    @Mock private Ec2InstanceProvisioner applierProvisioner;
    @Mock private Ec2InstanceProvisioner txManagerProvisioner;

    private ReplicaLifecycleManager replicaLifecycleManager;

    @BeforeEach
    void setUp() {
        replicaLifecycleManager = new ReplicaLifecycleManager(
                replicaConfig,
                replicaRepository,
                writeSchemaService,
                ec2Client,
                applierProvisionerFactory,
                storageEngineProvisionerFactory,
                txManagerProvisionerFactory
        );
    }

    // ── moveReplicaFromNewToProvisioning ──────────────────────────────────────

    @Test
    void moveFromNewToProvisioning_writeSchemaNotFound_throwsIllegalStateException() {
        when(replicaRepository.findByStatus(ReplicaStatus.NEW)).thenReturn(java.util.List.of(NEW_REPLICA));
        when(writeSchemaService.findByUserIdAndChronicleName(USER_ID, CHRONICLE_NAME)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> replicaLifecycleManager.moveFromNewToProvisioning())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(USER_ID)
                .hasMessageContaining(CHRONICLE_NAME);
    }

    @Test
    void moveFromNewToProvisioning_storageEngineProvisioningFails_savesErrorAndTerminates() {
        final WriteSchema writeSchema = new WriteSchema("ws-123", USER_ID, CHRONICLE_NAME, "{}", Instant.now());
        when(replicaRepository.findByStatus(ReplicaStatus.NEW)).thenReturn(java.util.List.of(NEW_REPLICA));
        when(writeSchemaService.findByUserIdAndChronicleName(USER_ID, CHRONICLE_NAME)).thenReturn(java.util.Optional.of(writeSchema));
        when(storageEngineProvisionerFactory.forType(ReplicaType.REDIS)).thenReturn(storageEngineProvisioner);
        when(storageEngineProvisioner.provision(any())).thenThrow(new RuntimeException("EC2 error"));

        replicaLifecycleManager.moveFromNewToProvisioning();

        verify(replicaRepository).save(NEW_REPLICA.withStatus(ReplicaStatus.ERROR));
        verify(ec2Client, never()).terminateInstances(any(TerminateInstancesRequest.class));
    }

    @Test
    void moveFromNewToProvisioning_storageEngineProvisionedButWaitFails_terminatesStorageEngine() {
        final WriteSchema writeSchema = new WriteSchema("ws-123", USER_ID, CHRONICLE_NAME, "{}", Instant.now());
        when(replicaRepository.findByStatus(ReplicaStatus.NEW)).thenReturn(java.util.List.of(NEW_REPLICA));
        when(writeSchemaService.findByUserIdAndChronicleName(USER_ID, CHRONICLE_NAME)).thenReturn(java.util.Optional.of(writeSchema));
        when(storageEngineProvisionerFactory.forType(ReplicaType.REDIS)).thenReturn(storageEngineProvisioner);
        when(storageEngineProvisioner.provision(any())).thenReturn(STORAGE_ID);
        when(ec2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .thenThrow(new RuntimeException("describe failed"));

        replicaLifecycleManager.moveFromNewToProvisioning();

        verify(ec2Client).terminateInstances(TerminateInstancesRequest.builder()
                .instanceIds(STORAGE_ID)
                .build());
        verify(replicaRepository).save(NEW_REPLICA.withStatus(ReplicaStatus.ERROR));
    }

    @Test
    void moveFromNewToProvisioning_success_savesProvisioningStatusWithInstanceIds() {
        final WriteSchema writeSchema = new WriteSchema("ws-123", USER_ID, CHRONICLE_NAME, "{}", Instant.now());
        when(replicaRepository.findByStatus(ReplicaStatus.NEW)).thenReturn(java.util.List.of(NEW_REPLICA));
        when(writeSchemaService.findByUserIdAndChronicleName(USER_ID, CHRONICLE_NAME)).thenReturn(java.util.Optional.of(writeSchema));
        when(storageEngineProvisionerFactory.forType(ReplicaType.REDIS)).thenReturn(storageEngineProvisioner);
        when(applierProvisionerFactory.forType(any(), any(), any(), any())).thenReturn(applierProvisioner);
        when(txManagerProvisionerFactory.forType(any(), any(), any(), any())).thenReturn(txManagerProvisioner);
        when(storageEngineProvisioner.provision(any())).thenReturn(STORAGE_ID);
        when(applierProvisioner.provision(any())).thenReturn(APPLIER_ID);
        when(txManagerProvisioner.provision(any())).thenReturn(TX_MANAGER_ID);
        when(ec2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(runningInstanceResponse(PUBLIC_IP));

        replicaLifecycleManager.moveFromNewToProvisioning();

        verify(replicaRepository).save(NEW_REPLICA
                .withApplierInstanceId(APPLIER_ID)
                .withStorageEngineInstanceId(STORAGE_ID)
                .withTxManagerInstanceId(TX_MANAGER_ID)
                .withStatus(ReplicaStatus.PROVISIONING));
    }

    // ── moveReplicaFromProvisioningToRunning ──────────────────────────────────

    @Test
    void moveFromProvisioningToRunning_exceededTimeoutWithPendingInstances_terminatesAndSavesError() {
        final Replica timedOutReplica = new Replica(
                "replica-123", USER_ID, CHRONICLE_ID, CHRONICLE_NAME, ReplicaType.REDIS,
                APPLIER_ID, STORAGE_ID, TX_MANAGER_ID,
                null, ReplicaStatus.PROVISIONING, Instant.now().minusSeconds(700)
        );
        when(replicaRepository.findByStatus(ReplicaStatus.PROVISIONING)).thenReturn(java.util.List.of(timedOutReplica));
        when(ec2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(pendingInstanceResponse());

        replicaLifecycleManager.moveFromProvisioningToRunning();

        verify(ec2Client).terminateInstances(TerminateInstancesRequest.builder()
                .instanceIds(APPLIER_ID, STORAGE_ID, TX_MANAGER_ID)
                .build());
        verify(replicaRepository).save(timedOutReplica.withStatus(ReplicaStatus.ERROR));
    }

    @Test
    void moveFromProvisioningToRunning_notAllInstancesRunning_doesNotPromote() {
        when(replicaRepository.findByStatus(ReplicaStatus.PROVISIONING)).thenReturn(java.util.List.of(PROVISIONING_REPLICA));
        when(ec2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(pendingInstanceResponse());

        replicaLifecycleManager.moveFromProvisioningToRunning();

        verify(replicaRepository, never()).save(any());
    }

    @Test
    void moveFromProvisioningToRunning_allRunningButPortClosed_doesNotPromote() {
        when(replicaRepository.findByStatus(ReplicaStatus.PROVISIONING)).thenReturn(java.util.List.of(PROVISIONING_REPLICA));
        when(ec2Client.describeInstances(any(DescribeInstancesRequest.class)))
                .thenReturn(runningInstanceResponse(PUBLIC_IP));

        replicaLifecycleManager.moveFromProvisioningToRunning();

        verify(replicaRepository, never()).save(any());
    }

    @Test
    void moveFromProvisioningToRunning_allRunningAndPortsOpen_savesRunningStatus() throws IOException {
        try (final ServerSocket txSocket = new ServerSocket(0)) {

            when(replicaRepository.findByStatus(ReplicaStatus.PROVISIONING)).thenReturn(java.util.List.of(PROVISIONING_REPLICA));
            when(ec2Client.describeInstances(any(DescribeInstancesRequest.class)))
                    .thenReturn(runningInstanceResponse("127.0.0.1"));
            when(replicaConfig.txManagerPort()).thenReturn(txSocket.getLocalPort());

            replicaLifecycleManager.moveFromProvisioningToRunning();

            verify(replicaRepository).save(PROVISIONING_REPLICA
                    .withStatus(ReplicaStatus.RUNNING)
                    .withTxManagerPublicIp("127.0.0.1"));
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private DescribeInstancesResponse runningInstanceResponse(String ip) {
        return DescribeInstancesResponse.builder()
                .reservations(Reservation.builder()
                        .instances(Instance.builder()
                                .state(InstanceState.builder().name(InstanceStateName.RUNNING).build())
                                .publicIpAddress(ip)
                                .privateIpAddress(ip)
                                .build())
                        .build())
                .build();
    }

    private DescribeInstancesResponse pendingInstanceResponse() {
        return DescribeInstancesResponse.builder()
                .reservations(Reservation.builder()
                        .instances(Instance.builder()
                                .state(InstanceState.builder().name(InstanceStateName.PENDING).build())
                                .build())
                        .build())
                .build();
    }
}