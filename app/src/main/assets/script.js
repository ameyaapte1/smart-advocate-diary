 'use strict';

xmlHTTP_send_post = (function() {
    var cached_function = xmlHTTP_send_post;

    return function(url,param) {
        // your code

        var res = cached_function.apply(this, arguments); // use .apply() to call it

        if(url=='o_civil_case_history.php'){
            app.showCaseDetails(param,res);
        }
        // more of your code

        return res;
    };
})();