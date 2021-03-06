/*
 * Copyright (c) 2010 Lockheed Martin Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eurekastreams.server.action.execution.notification.translator;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eurekastreams.server.domain.NotificationDTO;
import org.eurekastreams.server.domain.NotificationType;
import org.eurekastreams.server.domain.stream.ActivityDTO;
import org.eurekastreams.server.domain.stream.StreamEntityDTO;
import org.eurekastreams.server.persistence.mappers.DomainMapper;
import org.eurekastreams.server.persistence.mappers.db.GetCommentorIdsByActivityId;
import org.eurekastreams.server.search.modelview.CommentDTO;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the group comment notification translator.
 */
public class GroupCommentTranslatorTest
{
    /** Context for building mock objects. */
    private final Mockery context = new JUnit4Mockery()
    {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    /** Mock commentors mapper. */
    private final GetCommentorIdsByActivityId commentorsMapper = context.mock(GetCommentorIdsByActivityId.class);

    /** Mock activities mapper. */
    private final DomainMapper<List<Long>, List<ActivityDTO>> activitiesMapper = context.mock(DomainMapper.class,
            "activitiesMapper");

    /** Mock activities mapper. */
    private final DomainMapper<Long, List<Long>> coordinatorsMapper = context.mock(DomainMapper.class);

    /** Mapper to get the comment. */
    private final DomainMapper<List<Long>, List<CommentDTO>> commentsMapper = context.mock(DomainMapper.class,
            "commentsMapper");

    /** Mapper to get the savers. */
    private final DomainMapper<Long, List<Long>> saversMapper = context.mock(DomainMapper.class, "saversMapper");

    /** System under test. */
    private GroupCommentTranslator sut;

    /** Test data. */
    private static final long ACTOR_ID = 1111L;

    /** Test data. */
    private static final long STREAM_OWNER_ID = 2222L;

    /** Test data. */
    private static final long DESTINATION_ID = 3333L;

    /** Test data. */
    private static final long ACTIVITY_ID = 4444L;

    /** Test data. */
    private static final long COMMENT_ID = 4545L;

    /** Test data. */
    private static final long COMMENTOR = 5555L;

    /** Test data. */
    private static final long COORDINATOR_ID = 6666L;

    /** Test data. */
    private static final long SAVER = 7777L;

    /**
     * Setup before each test.
     */
    @Before
    public void setUp()
    {
        sut = new GroupCommentTranslator(commentorsMapper, activitiesMapper, coordinatorsMapper, commentsMapper,
                saversMapper);
    }

    /**
     * Test the translator.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testTranslateWithCoordinators()
    {
        final StreamEntityDTO actor = new StreamEntityDTO();
        actor.setId(STREAM_OWNER_ID);

        final ActivityDTO activity = new ActivityDTO();
        activity.setActor(actor);

        final List<Long> coordinators = Arrays.asList(COORDINATOR_ID);

        final CommentDTO comment = new CommentDTO();
        comment.setActivityId(ACTIVITY_ID);

        context.checking(new Expectations()
        {
            {
                oneOf(commentsMapper).execute(Collections.singletonList(COMMENT_ID));
                will(returnValue(Collections.singletonList(comment)));

                oneOf(coordinatorsMapper).execute(DESTINATION_ID);
                will(returnValue(new ArrayList(coordinators)));

                oneOf(activitiesMapper).execute(Arrays.asList(ACTIVITY_ID));
                will(returnValue(Arrays.asList(activity)));

                oneOf(commentorsMapper).execute(ACTIVITY_ID);
                will(returnValue(Collections.singletonList(COMMENTOR)));

                oneOf(saversMapper).execute(ACTIVITY_ID);
                will(returnValue(Arrays.asList(ACTOR_ID, STREAM_OWNER_ID, COMMENTOR, SAVER)));
            }
        });

        Collection<NotificationDTO> results = sut.translate(ACTOR_ID, DESTINATION_ID, COMMENT_ID);
        assertEquals(4, results.size());
        context.assertIsSatisfied();

        // put notifs in a map to easily get by expected type
        Map<NotificationType, NotificationDTO> notifs = new HashMap<NotificationType, NotificationDTO>();
        for (NotificationDTO notif : results)
        {
            notifs.put(notif.getType(), notif);
        }
        // check COMMENT_TO_SAVED_POST notif
        NotificationDTO notif = notifs.get(NotificationType.COMMENT_TO_SAVED_POST);
        assertNotNull(notif);
        assertEquals(1, notif.getRecipientIds().size());
        assertEquals((Long) SAVER, notif.getRecipientIds().get(0));
    }

    /**
     * Test the translator.
     */
    @Test
    public void testTranslateWithoutCoordinators()
    {
        sut = new GroupCommentTranslator(commentorsMapper, activitiesMapper, null, commentsMapper, saversMapper);

        final StreamEntityDTO actor = new StreamEntityDTO();
        actor.setId(STREAM_OWNER_ID);

        final ActivityDTO activity = new ActivityDTO();
        activity.setActor(actor);

        final CommentDTO comment = new CommentDTO();
        comment.setActivityId(ACTIVITY_ID);

        context.checking(new Expectations()
        {
            {
                oneOf(commentsMapper).execute(Collections.singletonList(COMMENT_ID));
                will(returnValue(Collections.singletonList(comment)));

                oneOf(activitiesMapper).execute(Collections.singletonList(ACTIVITY_ID));
                will(returnValue(Collections.singletonList(activity)));

                oneOf(commentorsMapper).execute(ACTIVITY_ID);
                will(returnValue(Collections.singletonList(COMMENTOR)));

                oneOf(saversMapper).execute(ACTIVITY_ID);
                will(returnValue(Arrays.asList(ACTOR_ID, STREAM_OWNER_ID, COMMENTOR, SAVER)));
            }
        });

        Collection<NotificationDTO> results = sut.translate(ACTOR_ID, DESTINATION_ID, COMMENT_ID);
        assertEquals(3, results.size());
        context.assertIsSatisfied();

        // put notifs in a map to easily get by expected type
        Map<NotificationType, NotificationDTO> notifs = new HashMap<NotificationType, NotificationDTO>();
        for (NotificationDTO notif : results)
        {
            notifs.put(notif.getType(), notif);
        }
        // check COMMENT_TO_SAVED_POST notif
        NotificationDTO notif = notifs.get(NotificationType.COMMENT_TO_SAVED_POST);
        assertNotNull(notif);
        assertEquals(1, notif.getRecipientIds().size());
        assertEquals((Long) SAVER, notif.getRecipientIds().get(0));
    }

    /**
     * Test the translator.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testTranslateCommentNotFound()
    {
        context.checking(new Expectations()
        {
            {
                oneOf(commentsMapper).execute(Collections.singletonList(COMMENT_ID));
                will(returnValue(Collections.EMPTY_LIST));
            }
        });

        Collection<NotificationDTO> results = sut.translate(ACTOR_ID, DESTINATION_ID, COMMENT_ID);
        context.assertIsSatisfied();
        assertTrue(results.isEmpty());
    }

    /**
     * Test the translator.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testTranslateActivityNotFound()
    {
        final CommentDTO comment = new CommentDTO();
        comment.setActivityId(ACTIVITY_ID);

        context.checking(new Expectations()
        {
            {
                oneOf(commentsMapper).execute(Collections.singletonList(COMMENT_ID));
                will(returnValue(Collections.singletonList(comment)));

                oneOf(activitiesMapper).execute(Collections.singletonList(ACTIVITY_ID));
                will(returnValue(Collections.EMPTY_LIST));
            }
        });

        Collection<NotificationDTO> results = sut.translate(ACTOR_ID, DESTINATION_ID, COMMENT_ID);
        context.assertIsSatisfied();
        assertTrue(results.isEmpty());
    }
}
