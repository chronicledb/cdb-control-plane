package io.github.grantchen2003.cdb.control.plane.replicas.provisioning;

import io.github.grantchen2003.cdb.control.plane.config.ReplicaConfig;
import io.github.grantchen2003.cdb.control.plane.replicas.ReplicaType;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.redis.RedisTxManagerProvisioner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ec2.Ec2Client;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TxManagerProvisionerFactoryTest {

    @Mock
    private Ec2Client ec2Client;

    @Mock
    private ReplicaConfig replicaConfig;

    @InjectMocks
    private TxManagerProvisionerFactory factory;

    @Test
    void forType_redis_returnsRedisTxManagerProvisioner() {
        final Ec2InstanceProvisioner provisioner = factory.forType(ReplicaType.REDIS);
        assertThat(provisioner).isInstanceOf(RedisTxManagerProvisioner.class);
    }
}