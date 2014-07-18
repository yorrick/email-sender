db.smslist.update(
    {},
    { $set: {"status" : "AckedByMailgun"}},
    {multi: true}
)