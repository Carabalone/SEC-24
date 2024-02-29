from cryptography.hazmat.primitives import serialization as crypto_serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.backends import default_backend as crypto_default_backend
import sys

# Load or initialize counter
try:
    with open('counter.txt', 'r') as counter_file:
        counter = int(counter_file.read())
except FileNotFoundError:
    print("cannot find / tampered counter.txt")
    sys.exit(0)

# Generate new key pair
key = rsa.generate_private_key(
    backend=crypto_default_backend(),
    public_exponent=65537,
    key_size=2048
)

private_key = key.private_bytes(
    crypto_serialization.Encoding.PEM,
    crypto_serialization.PrivateFormat.PKCS8,
    crypto_serialization.NoEncryption()
)

public_key = key.public_key().public_bytes(
    crypto_serialization.Encoding.OpenSSH,
    crypto_serialization.PublicFormat.OpenSSH
)

# Save key pair to files
private_key_file = f'keys/key_{counter}_private.pem'
public_key_file = f'keys/key_{counter}_public.pem'

with open(private_key_file, 'wb') as f:
    f.write(private_key)

with open(public_key_file, 'wb') as f:
    f.write(public_key)

# Increment counter
counter += 1

# Save updated counter
with open('counter.txt', 'w') as counter_file:
    counter_file.write(str(counter))
