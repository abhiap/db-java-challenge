# db-challenge 

Account and Transfer API 
- Existing /accounts api. Added custom runtime exception to handle account-not-found scenario
- Added /transactions/transfer api

Code tested with junits and via postman
- TransactionServiceTest 
- TransactionControllerTest (not fully functional)

Added dockerfile (not tested) 

Sample requests:

1: Create Account
curl --request POST \
  --url http://localhost:18080/v1/accounts \
  --header 'Content-Type: application/json' \
  --data '{
	"accountId": "ID-101",
	"balance": "100.5999"
}'

2: Get Account
curl --request GET \
  --url http://localhost:18080/v1/accounts/ID-101
  
3: Transfer amount from one account to another 
curl --request POST \
  --url 'http://localhost:18080/v1/transactions/transfer?fromAccountId=ID-101222&toAccountId=ID-101&amountToTransfer=5.09' \
  --header 'Content-Type: application/json'
