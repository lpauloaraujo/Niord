from src.main import app


app.description = """## Authentication Flow
Para registrar e autenticar um usuário realize os seguintes passos

1. **Registrar**: `POST /auth/register` com um request **body** como descrito no exemplo do endpoint.
    1. A string da placa deve omitir qualquer "-" ou "." para ser válidada, mesmo que seja uma placa no padrão antigo.
    2. Cria uma entrada temporária no banco Redis com os dados do usuário.
    3. Cria o código OTP no banco Redis e realiza o envio do email ao que foi fornecido.
2. **Confirmar código**: `POST /auth/verify?email=&code=` uma **query** contendo o email e código.
    1. Realiza a criação do usuário no banco SQL e marca como verificado.
    2. O código é excluído/invalidado do banco Redis.
    2. Para reenviar o código realizar `POST /auth/resend?email=&password=` uma **query** contendo email e senha.
        1. A senha é validada através da comparação com o hash armazenado temporariamente no banco Redis.
3. **Login**: `POST /auth/login?email=&password=` uma **query** contendo email e senha.
    1. Será retornado como resposta um JSON com dois valores contendo os tokens `refresh` e `access`.
    2. O token `access` possui vida curta de alguns minutos e serve como um identificador confiável e temporário do usuário, ao fazer login ele tem propriedade `fresh=True` sinalizando que as credenciais foram utilizadas na criação do token.
    3. O token `refresh` é o token principal e duradouro, sendo a sessão do usuário, ele é utilizado em `/auth/refresh` para atualizar o token `access`.
4. **Refresh**: `POST /auth/refresh?refresh_token=` uma **query** contendo o token refresh do usuário.
    1. Será retornado como resposta um JSON contendo dois valores `refresh` e `access`, onde `refresh` pode ser **null**, significando que o `refresh` não necessita de atualizações, caso seja retornado um `refresh` é necessário que o mesmo seja considerado como o corrente.
    2. O valor `access` retornado é um token access válido mas com propriedade `fresh=False`, por não ser criado com credenciais do usuário.
    3. O endpoint admite um **body** adicional no request contendo `email` e `password`, onde serão validados e será retornado um `access` com propriedade `fresh=True`.

- O token `access` será o token utilizado para identificar o usuário, ele é decodificado de forma independente do banco de dados, contendo informações como id ou roles do usuário.
- O token `refresh` define a sessão do usuário, sendo utilizado para gerar tokens de acesso, sendo feitas consultas no banco de dados para validar o token refresh e codificar o token access com informações úteis.

"""
