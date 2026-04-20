package io.github.grantchen2003.cdb.control.plane.replicas.provisioning;

import io.github.grantchen2003.cdb.control.plane.replicas.ReplicaType;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.redis.RedisApplierProvisioner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ApplierProvisionerFactoryTest {

    @InjectMocks
    private ApplierProvisionerFactory factory;

    @Test
    void forType_redis_returnsRedisApplierProvisioner() {
        final Ec2InstanceProvisioner provisioner = factory.forType(
                ReplicaType.REDIS,
                "chronicle-123",
                "{}",
                "localhost");
        assertThat(provisioner).isInstanceOf(RedisApplierProvisioner.class);
    }
}