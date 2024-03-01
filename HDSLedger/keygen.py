from cryptography.hazmat.primitives import serialization as crypto_serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.backends import default_backend as crypto_default_backend
import sys
import os

def generate_key_pairs(name, num_pairs):
    # Create directory if it doesn't exist
    if not os.path.exists(f'keys/{name}'):
        os.makedirs(f'keys/{name}')

    for i in range(num_pairs):
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
        private_key_file = f'keys/{name}/key_{i+1}_private.pem'
        public_key_file = f'keys/{name}/key_{i+1}_public.pem'
        print(f'generating key in keys/{name} {i+1}/{num_pairs}')

        with open(private_key_file, 'wb') as f:
            f.write(private_key)

        with open(public_key_file, 'wb') as f:
            f.write(public_key)

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python3 keygen.py <name> <num_pairs>")
        sys.exit(1)

    name = sys.argv[1]
    num_pairs = int(sys.argv[2])

    generate_key_pairs(name, num_pairs)
