import os

def clean_keys_directory(directory):
    for root, dirs, files in os.walk(directory):
        for file_name in files:
            if file_name.startswith("key_") and (file_name.endswith("_private.pem") or file_name.endswith("_public.pem")):
                file_path = os.path.join(root, file_name)
                os.remove(file_path)
                print(f"Removed: {file_path}")

# Specify the directory to clean
directory_to_clean = "keys"

# Call the function to clean the keys directory
clean_keys_directory(directory_to_clean)
