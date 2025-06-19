# income-tax-gift-aid

This is where we  make API calls from users for creating, viewing and updating the donations to charity section of their income tax return.

### Running the service locally

You will need to have the following:
- Installed [MongoDB](https://docs.mongodb.com/manual/installation/)
- Installed/configured [service manager V2](https://github.com/hmrc/sm2).

This can be found in the [developer handbook](https://docs.tax.service.gov.uk/mdtp-handbook/documentation/developer-set-up/)

The service manager profile is:

    sm2 --start INCOME_TAX_GIFT_AID

Run the following command to start the remaining services locally:

    sudo mongod (If not already running)
    sm2 --start INCOME_TAX_SUBMISSION_ALL

This service runs on port: `localhost:9316`

### Running Tests
- Run Unit Tests:  `sbt test`
- Run Integration Tests: `sbt it/test`
- Run Unit and Integration Tests: `sbt test it/test`
- Run Unit and Integration Tests with coverage report: `sbt runAllChecks`<br/>
  which runs `clean compile coverage test it/test coverageReport dependencyUpdates`

### Feature Switches
| Feature                         | Description                                                      |
|---------------------------------|------------------------------------------------------------------|
| sectionCompletedQuestionEnabled | Redirects user to Have you completed this section from CYA page  |

### Gift-aid endpoints:

- **GET     /income-tax/nino/:nino/income-source/charity/annual/:taxYear** (Retrieves details for the charity income source over the accounting period which matches the tax year provided)
- **POST    /income-tax/nino/:nino/income-source/charity/annual/:taxYear** (Provides the ability for a user to submit periodic annual income for charity)

### Downstream services:

All donations to charity data is retrieved/updated via the downstream system:

- DES (Data Exchange Service)

### Upstream services:

Personal-Income-Tax-Submission-Frontend repository see its [readMe](https://github.com/hmrc/personal-income-tax-submission-frontend/blob/main/README.md) for more.

### Gift-aid income source

<details>

<summary>Click here to see an example of a users charity data (JSON)</summary>

```json
{
  "giftAidPayments": {
      "nonUkCharitiesCharityNames": [
        "Charity 1"
      ],
      "currentYear": 23426505146.99,
      "oneOffCurrentYear": 80331713889.99,
      "currentYearTreatedAsPreviousYear": 44753493320.99,
      "nextYearTreatedAsCurrentYear": 88970014371.99,
      "nonUkCharities": 77143081269.00
  },
  "gifts": {
      "investmentsNonUkCharitiesCharityNames": [
        "Charity 2"
      ],
      "landAndBuildings": 11200049718.00,
      "sharesOrSecurities": 82198960626.00,
      "investmentsNonUkCharities": 24966390172.00
  }
}
```
</details>

## Ninos with stubbed data for gift-aid

| Nino      | Gift-aid data           |
|-----------|-------------------------|
| AA123459A | User with gift-aid data |
| AA637489D | User with gift-aid data |


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
