package ru.mail.jira.plugins.groovy.impl.jql.function.builtin;

import com.atlassian.jira.issue.search.SearchProviderFactory;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.query.IssueIdJoinQueryFactory;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.jql.query.QueryCreationContextImpl;
import com.atlassian.jira.jql.query.QueryFactoryResult;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.FunctionOperand;
import com.atlassian.query.operator.Operator;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.groovy.impl.jql.function.builtin.query.QueryParseResult;
import ru.mail.jira.plugins.groovy.impl.jql.function.builtin.query.WorkLogQueryParser;

import javax.annotation.Nonnull;
import java.util.List;

@Component
public class WorkLoggedFunction extends AbstractBuiltInQueryFunction {
    private final Logger logger = LoggerFactory.getLogger(WorkLoggedFunction.class);

    private final IssueIdJoinQueryFactory issueIdJoinQueryFactory;
    private final WorkLogQueryParser workLogQueryParser;

    @Autowired
    public WorkLoggedFunction(
        IssueIdJoinQueryFactory issueIdJoinQueryFactory,
        WorkLogQueryParser workLogQueryParser
    ) {
        super("workLogged", 1);
        this.issueIdJoinQueryFactory = issueIdJoinQueryFactory;
        this.workLogQueryParser = workLogQueryParser;
    }

    @Override
    protected void validate(MessageSet messageSet, ApplicationUser user, @Nonnull FunctionOperand functionOperand, @Nonnull TerminalClause terminalClause) {
        QueryParseResult parseResult = workLogQueryParser.parseParameters(
            new QueryCreationContextImpl(user), functionOperand.getArgs().get(0)
        );

        messageSet.addMessageSet(parseResult.getMessageSet());
    }

    @Nonnull
    @Override
    public QueryFactoryResult getQuery(@Nonnull QueryCreationContext queryCreationContext, @Nonnull TerminalClause terminalClause) {
        FunctionOperand functionOperand = (FunctionOperand) terminalClause.getOperand();
        List<String> args = functionOperand.getArgs();

        QueryParseResult parseResult = workLogQueryParser.parseParameters(queryCreationContext, args.get(0));

        if (parseResult.hasErrors()) {
            logger.error("Got errors while building query: {}", parseResult.getMessageSet());
            return QueryFactoryResult.createFalseResult();
        }

        return new QueryFactoryResult(
            issueIdJoinQueryFactory.createIssueIdJoinQuery(parseResult.getQuery(), SearchProviderFactory.WORKLOG_INDEX),
            terminalClause.getOperator() == Operator.NOT_IN
        );
    }

    @Nonnull
    @Override
    public List<QueryLiteral> getValues(
        @Nonnull QueryCreationContext queryCreationContext,
        @Nonnull FunctionOperand functionOperand,
        @Nonnull TerminalClause terminalClause
    ) {
        return ImmutableList.of();
    }
}
