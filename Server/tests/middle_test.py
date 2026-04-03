from src.middle.user import hash_password, check_hash

def test_password_hash():
    test_password = "secretPassword"
    hashed_password = hash_password(test_password)
    assert not check_hash(test_password, hash_password("mock")) 
    assert check_hash(test_password, hashed_password)


