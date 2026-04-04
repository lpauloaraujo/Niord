from src.middle.user import hash_password, check_hash
from src.middle.auth import decode_token, encode_token
from datetime import datetime, timezone, timedelta
from random import randint

def test_password_hash():
    test_password = "secretPassword"
    hashed_password = hash_password(test_password)
    assert not check_hash(test_password, hash_password("mock")) 
    assert check_hash(test_password, hashed_password)

def test_token_encoding():
    test_id = randint(1, 9999)
    test_exp = datetime.now(timezone.utc) + timedelta(seconds=randint(50, 999))
    encoded = encode_token(test_id, 
                           test_exp, 
                           True)
    decoded = decode_token(encoded)
    assert decoded is not None
    assert decoded.id == test_id
    #The raw datetime is different, when encoding it looses the milliseconds info
    #It's to be expected
    assert int(decoded.exp.timestamp()) == int(test_exp.timestamp())
    assert decoded.fresh

def test_token_expiration():
    test_id = randint(1, 9999)
    #Should always be expired
    test_exp = datetime.now(timezone.utc) + timedelta(seconds=randint(-999, -50))
    encoded = encode_token(test_id, test_exp, None)
    decoded = decode_token(encoded)
    assert decoded is None

