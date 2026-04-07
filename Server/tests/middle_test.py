from src.middle.user import hash_password, check_hash, check_cpf_format, split_cpf, check_cpf_validity
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


def test_cpf_split():
    test_cpf = "123.456.789-00"
    test_cpf_bad = "12.34,56-78"
    test_cpf_bad_2 = "123456"
    assert split_cpf(test_cpf) == ["123", "456", "789", "00"]
    assert split_cpf(test_cpf_bad) == ["12", "34,56", "78"]
    assert split_cpf(test_cpf_bad_2) == ["123456"]

def test_cpf_format():
    test_cpf_good = "123.456.789-00"
    test_cpf_bad = ["123.456-22", "123.456-00", "123.456.789-0", "12.34.56-00"]
    assert check_cpf_format(split_cpf(test_cpf_good))
    for c in test_cpf_bad:
        assert not check_cpf_format(split_cpf(c))

def test_cpf_validator():
    test_cpf_good = ["529.982.247-25"]
    test_cpf_bad = ["123.456.789-00", "123.456.999-00", "111.111.111-11", "222.222.222-22"]
    for cpf in test_cpf_good:
        assert check_cpf_validity(split_cpf(cpf))
    for cpf in test_cpf_bad:
        assert not check_cpf_validity(split_cpf(cpf))










