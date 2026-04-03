from src.models.user import User, UserCredentials 

def test_credential_dump():
    userData = UserCredentials(
            name="testName",
            email="user@internet.com",
            password="Pass",
            registration_plate="ABC-123",
            cpf="123456789",
            telephone="123567",
            blood_type="A+"
            )
    data = userData.model_dump()
    user = User(**data, is_verified=False)
    data.pop("blood_type")
    #Checks if Nullable blood_type applies
    user_no_blood_type = User(**data, is_verified=False)

