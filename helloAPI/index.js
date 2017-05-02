// Authors:
// Shane Oatman https://github.com/shoatman
// Sunil Bandla https://github.com/sunilbandla
// Daniel Dobalian https://github.com/danieldobalian

var express = require("express");
var morgan = require("morgan");
var passport = require("passport");
var BearerStrategy = require('passport-azure-ad').BearerStrategy;

// TODO: Update the first 3 variables
var tenantID = "fabrikamb2c.onmicrosoft.com";
var clientID = "25eef6e4-c905-4a07-8eb4-0d08d5df8b3f";
var policyName = "B2C_1_SUSI";

var options = {
    identityMetadata: "https://login.microsoftonline.com/" + tenantID + "/v2.0/.well-known/openid-configuration/",
    clientID: clientID,
    policyName: policyName,
    isB2C: true,
    validateIssuer: false,
    scope: ['demo.read'],
    loggingLevel: 'info',
    passReqToCallback: false
};

var bearerStrategy = new BearerStrategy(options,
    function (token, done) {
        // Send user info using the second argument
        done(null, {}, token);
    }
);

var app = express();
app.use(morgan('dev'));

app.use(passport.initialize());
passport.use(bearerStrategy);

app.use(function (req, res, next) {
    res.header("Access-Control-Allow-Origin", "*");
    res.header("Access-Control-Allow-Headers", "Authorization, Origin, X-Requested-With, Content-Type, Accept");
    next();
});

app.get("/hello",
    passport.authenticate('oauth-bearer', {session: false}),
    function (req, res) {
        var claims = req.authInfo;
        console.log('User info: ', req.user);
        console.log('Validated claims: ', claims);
        // var claimsList = Object.keys(claims)
        //     .reduce(function (previous, key) {
        //         if (key == 'name') {
        //             return previous.concat({
        //                 type: key,
        //                 value: claims[key]
        //             });
        //         }
        //     }, []);
        res.status(200).json({'name': claims['name']});
    }
);

app.listen(5000, function () {
    console.log("Listening on port 5000");
});
